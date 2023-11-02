package busrouting.main.data;

import busrouting.configLoading.FileExceptions;
import busrouting.configLoading.FileExceptionLoader;
import busrouting.configLoading.LocationNameOverlap;
import busrouting.configLoading.TransferException;
import tech.tablesaw.api.*;

import static tech.tablesaw.aggregate.AggregateFunctions.*;

import java.io.IOException;
import java.util.List;

public class DataPreProcessorStandardData {

    public DataPreProcessorStandardData() {
    }

    //days 1-7 -> monday-sunday
    public RoutingDataProcessed preProcess(RoutingDataStandardData rawRoutingDataStandardData, int weekDay) throws IOException {
        Table locationInfo = rawRoutingDataStandardData.getLocationInfo();
        Table scheduleDataDayOne = rawRoutingDataStandardData.getScheduleData();
        Table scheduleDataDayTwo = rawRoutingDataStandardData.getScheduleData();
        Table drivingTimes = rawRoutingDataStandardData.getDrivingTimes();

        //read exceptions for the csv files
        FileExceptionLoader fileExceptionLoader = new FileExceptionLoader("fileExceptionsStandardData.yaml");
        FileExceptions fileExceptions = fileExceptionLoader.getStoppingPointExceptions();

        //cut not relevant data & remove betriebshöfe & remove teststeige + e ladestationen (index 0 based instead 1 based as in r)
        locationInfo = locationInfo.retainColumns("ONR_TYP_NR", "ORT_NR", "ORT_REF_ORT", "ORT_REF_ORT_NAME");
        locationInfo = locationInfo.where(locationInfo.intColumn("ONR_TYP_NR").isEqualTo(1));
        locationInfo.removeColumns("ONR_TYP_NR");

        for(String name : locationInfo.stringColumn("ORT_REF_ORT_NAME")) {
            String oldName = name;
            name = name.replaceAll("\"", "");
            name = name.trim();
            locationInfo.stringColumn("ORT_REF_ORT_NAME").set(locationInfo.stringColumn("ORT_REF_ORT_NAME").isEqualTo(oldName), name);
        }
        if(fileExceptions.getRemoveStoppingPointsIdentifiers() == null) {
        } else {
            for (Integer identifier : fileExceptions.getRemoveStoppingPointsIdentifiers()) {
                locationInfo = locationInfo.dropWhere(locationInfo.intColumn("ORT_NR").isEqualTo(identifier));
            }
        }
        //differentiate not unique ort_ref_ort_name
        if(fileExceptions.getLocationNameOverlaps() == null) {
        } else {
            for (LocationNameOverlap lno : fileExceptions.getLocationNameOverlaps()) {
                locationInfo.stringColumn("ORT_REF_ORT_NAME").set(locationInfo.intColumn("ORT_REF_ORT").isEqualTo(lno.getIdentifier()), lno.getNewName());
            }
        }

        //create stopping points with transfer time
        Table transferTimetable = createTransferTimetable(locationInfo);

        //exceptions of transfer time
        if(fileExceptions.getTransferExceptions() == null){
        } else {
            for(TransferException tf : fileExceptions.getTransferExceptions()) {
                transferTimetable.intColumn("TRANSFER_TIME").set(transferTimetable.intColumn("ORT_REF_ORT").isEqualTo(tf.getIdentifier()), tf.getTransferTime());
            }
        }

        //only mondays & no betriebshof as target & cut relevant data
        int nextDay = weekDay;
        if(nextDay < 7) {
            nextDay++;
        } else {
            nextDay = 1;
        }
        scheduleDataDayOne = scheduleDataDayOne.where(scheduleDataDayOne.intColumn("TAGESART_NR").isEqualTo(weekDay));
        scheduleDataDayTwo = scheduleDataDayTwo.where(scheduleDataDayTwo.intColumn("TAGESART_NR").isEqualTo(nextDay));
        scheduleDataDayOne = scheduleDataDayOne.where(scheduleDataDayOne.intColumn("FAHRTART_NR").isEqualTo(1));
        scheduleDataDayTwo = scheduleDataDayTwo.where(scheduleDataDayTwo.intColumn("FAHRTART_NR").isEqualTo(1));
        scheduleDataDayOne = scheduleDataDayOne.retainColumns("FRT_START", "LI_NR", "FGR_NR");
        scheduleDataDayTwo = scheduleDataDayTwo.retainColumns("FRT_START", "LI_NR", "FGR_NR");

        //no betriebshof as start or target & cut not relevant data
        drivingTimes = drivingTimes.where(drivingTimes.intColumn("ONR_TYP_NR").isEqualTo(1));
        drivingTimes = drivingTimes.where(drivingTimes.intColumn("SEL_ZIEL_TYP").isEqualTo(1));
        drivingTimes = drivingTimes.retainColumns("FGR_NR", "ORT_NR", "SEL_ZIEL", "SEL_FZT");

        Table rideTimetable = createRideTimetable(drivingTimes, scheduleDataDayOne, fileExceptions);
        Table rideTimetableNextDay = createRideTimetable(drivingTimes, scheduleDataDayTwo, fileExceptions);

        return new RoutingDataProcessed(locationInfo, rideTimetable, transferTimetable, rideTimetableNextDay);
    }

    public Table createRideTimetable(Table drivingTimes, Table scheduleData, FileExceptions fileExceptions) {
        //maintain the stable order after sorting and join operation
        IntColumn ind = IntColumn.create("INDEX_SORT");
        for (int k = 1; k <= drivingTimes.rowCount(); k++) {
            ind.append(k);
        }
        drivingTimes.addColumns(ind);

        //create riding timetable (driving times per stopping point)
        Table rideTimetable = drivingTimes.joinOn("FGR_NR").inner(scheduleData);
        //remove all teststations etc.
        if(fileExceptions.getRemoveStoppingPointsIdentifiers() == null) {
        } else {
            for(Integer identifier : fileExceptions.getRemoveStoppingPointsIdentifiers()) {
                rideTimetable = rideTimetable.dropWhere(rideTimetable.intColumn("ORT_NR").isEqualTo(identifier).or(rideTimetable.intColumn("SEL_ZIEL").isEqualTo(identifier)));
            }
        }
        rideTimetable = rideTimetable.sortAscendingOn("FGR_NR", "FRT_START", "INDEX_SORT");
        rideTimetable.removeColumns("INDEX_SORT");
        drivingTimes.removeColumns("INDEX_SORT");
        //fill arival and departure
        IntColumn departureC = IntColumn.create("DEPARTURE");
        IntColumn arrivalC = IntColumn.create("ARRIVAL");
        int startCoursePrev = -1;
        int arrivePrev = 0;
        int fgrNrPrev = -1;
        for (int i = 0; i < rideTimetable.rowCount(); i++) {
            int startCourse = rideTimetable.intColumn("FRT_START").get(i);
            int rideTime = rideTimetable.intColumn("SEL_FZT").get(i);
            int fgrNr = rideTimetable.intColumn("FGR_NR").get(i);
            //if there is a new starting time -> start at starting time
            if ((startCoursePrev != startCourse) || (fgrNr != fgrNrPrev)) {
                departureC.append(startCourse);
                arrivalC.append(startCourse + rideTime);
                arrivePrev = startCourse + rideTime;
            } else { //if starting time is still the same -> calculate driving times
                departureC.append(arrivePrev);
                arrivalC.append(arrivePrev + rideTime);
                arrivePrev = arrivePrev + rideTime;
            }
            startCoursePrev = startCourse;
            fgrNrPrev = fgrNr;
        }
        rideTimetable.addColumns(departureC, arrivalC);

        rideTimetable.removeColumns("FGR_NR", "FRT_START");

        //make line nr a string column
        rideTimetable = rideTimetable.replaceColumn("LI_NR", rideTimetable.column("LI_NR").asStringColumn());
        // if added new name is "LI_NR strings"
        rideTimetable.stringColumn("LI_NR strings").setName("LI_NR");

        return rideTimetable;
    }

    //create table with transfer time dependend on number of sp per csp
    public Table createTransferTimetable(Table locationInfo) {
        Table temp = locationInfo.select("ORT_REF_ORT", "ORT_NR");
        Table transferTimetable = temp.summarize("ORT_NR", count).by("ORT_REF_ORT");
        transferTimetable = transferTimetable.sortAscendingOn("ORT_REF_ORT");
        transferTimetable.doubleColumn("Count [ORT_NR]").setName("FREQ");
        transferTimetable = transferTimetable.dropDuplicateRows();
        //add transfer time (number of sp-frequency minus one)
        IntColumn transferTime = IntColumn.create("TRANSFER_TIME");
        for (double frq : transferTimetable.doubleColumn("FREQ")) {
            transferTime.append((int)frq - 1);
        }
        transferTimetable.addColumns(transferTime);
        transferTimetable = transferTimetable.retainColumns("ORT_REF_ORT", "TRANSFER_TIME");
        return transferTimetable;
    }
}
