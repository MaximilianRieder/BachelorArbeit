package busrouting.main.data;

import busrouting.configLoading.FileExceptions;
import busrouting.configLoading.FileExceptionLoader;
import busrouting.configLoading.LocationNameOverlap;
import busrouting.configLoading.TransferException;
import tech.tablesaw.api.*;

import java.io.IOException;
import java.util.LinkedList;

public class DataPreProcessorTestData extends DataPreProcessorStandardData {

    public DataPreProcessorTestData() {
    }

    //day = 20200504 equals 04.05.2020
    public RoutingDataProcessed preProcess(RoutingDataTestData routingDataTestData, int day) throws IOException {
        Table stopData = routingDataTestData.getStopData();
        Table stopTimesData = routingDataTestData.getStopTimesData();
        Table routeData = routingDataTestData.getRouteData();
        Table tripData = routingDataTestData.getTripData();
        Table calendarData = routingDataTestData.getCalendarData();

        //read exceptions for the csv files
        FileExceptionLoader fileExceptionLoader = new FileExceptionLoader("fileExceptionsTestData.yaml");
        FileExceptions fileExceptions = fileExceptionLoader.getStoppingPointExceptions();

        Table locationInfo = createLocationInfo(stopData);
        //differentiate not unique ORT_REF_ORT_NAME
        if(fileExceptions.getLocationNameOverlaps() == null) {
        } else {
            for (LocationNameOverlap lno : fileExceptions.getLocationNameOverlaps()) {
                locationInfo.stringColumn("ORT_REF_ORT_NAME").set(locationInfo.intColumn("ORT_REF_ORT").isEqualTo(lno.getIdentifier()), lno.getNewName());
            }
        }
        if(fileExceptions.getRemoveStoppingPointsIdentifiers() == null) {
        } else {
            for (Integer identifier : fileExceptions.getRemoveStoppingPointsIdentifiers()) {
                locationInfo = locationInfo.dropWhere(locationInfo.intColumn("ORT_NR").isEqualTo(identifier));
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

        //rename special in special#1 in stopTimesData and calendarData
        replaceStringPartInColumn(stopTimesData, "trip_id", "Special-", "Special#1-");
        replaceStringPartInColumn(calendarData, "service_id", "Special#", "placeHolder");
        replaceStringPartInColumn(calendarData, "service_id", "Special", "Special#1");
        replaceStringPartInColumn(calendarData, "service_id", "placeHolder", "Special#");
        replaceStringPartInColumn(tripData,"trip_id", "Special-", "Special#1-");

        //transform data and rename ids
        Table stopTimesDataDayOne = processStopTimesData(stopTimesData, locationInfo, day, calendarData);
        Table stopTimesDataDayTwo = processStopTimesData(stopTimesData, locationInfo, (day + 1), calendarData);

        //riding Timetable
        Table rideTimetableTestData = createRideTimetable(stopTimesDataDayOne, routeData, tripData);
        Table rideTimetableTestDataNextDay = createRideTimetable(stopTimesDataDayTwo, routeData, tripData);
        if(fileExceptions.getRemoveStoppingPointsIdentifiers() == null) {
        } else {
            for(Integer identifier : fileExceptions.getRemoveStoppingPointsIdentifiers()) {
                rideTimetableTestData = rideTimetableTestData.dropWhere(rideTimetableTestData.intColumn("ORT_NR").isEqualTo(identifier).or(rideTimetableTestData.intColumn("SEL_ZIEL").isEqualTo(identifier)));
            }
        }

        //remove ORT_ID_OLD
        locationInfo.removeColumns("ORT_ID_OLD");

        return new RoutingDataProcessed(locationInfo, rideTimetableTestData, transferTimetable, rideTimetableTestDataNextDay);
    }

    //transform Data and rename ids
    public Table processStopTimesData(Table stopTimesData, Table locationInfo, int day, Table calendarData) {
        //remove lines that dont drive on this day
        LinkedList<String> dropIDs = getDropServiceIDs(calendarData, day);
        Table stopTimesDataProcess = stopTimesData.copy();
        for(String dropID : dropIDs) {
            stopTimesDataProcess = stopTimesDataProcess.dropWhere(stopTimesDataProcess.stringColumn("trip_id").containsString(dropID));
        }

        IntColumn stopTimesDataNewLocationID = IntColumn.create("ORT_NR");
        for(String oldID : stopTimesDataProcess.stringColumn("stop_id")) {
            int newId = Integer.parseInt(locationInfo.where(locationInfo.stringColumn("ORT_ID_OLD").isEqualTo(oldID)).getString(0, "ORT_NR"));
            stopTimesDataNewLocationID.append(newId);
        }
        stopTimesDataProcess.addColumns(stopTimesDataNewLocationID);

        DataTransformer dataTransformer = new DataTransformer(locationInfo);
        //make column with time as seconds
        IntColumn arrival = IntColumn.create("ARRIVAL");
        for(String arrivalString : stopTimesDataProcess.stringColumn("arrival_time")) {
            arrival.append(dataTransformer.makeSecondsFromTime(arrivalString));
        }
        stopTimesDataProcess.addColumns(arrival);
        IntColumn departure = IntColumn.create("DEPARTURE");
        for(String departureString : stopTimesDataProcess.stringColumn("departure_time")) {
            departure.append(dataTransformer.makeSecondsFromTime(departureString));
        }
        stopTimesDataProcess.addColumns(departure);


        return stopTimesDataProcess;
    }

    public Table createLocationInfo(Table stopData) {
        //create location info
        DoubleColumn ortNR = DoubleColumn.create("ORT_NR");
        DoubleColumn ortRefOrt = DoubleColumn.create("ORT_REF_ORT");
        StringColumn ortRefOrtName = StringColumn.create("ORT_REF_ORT_NAME");
        StringColumn ortIdOld = StringColumn.create("ORT_ID_OLD");
        for(Row row : stopData) {
            String stopIdString = row.getString("stop_id");
            //make column for renaming in other tables
            String stopIdStringOld = stopIdString;
            //ignore the Parent points
            if(stopIdString.endsWith("Parent"))
                continue;
            //remove other than de / gen
            if(stopIdString.startsWith("de:")) {
                //remove "de:"
                stopIdString = stopIdString.substring(3);
            } else if(stopIdString.startsWith("gen:")) {
                stopIdString = stopIdString.substring(4);
            } else {
                continue;
            }
            //change to numerical identifier
            if(stopIdString.endsWith("N")) {
                stopIdString = stopIdString.substring(0, stopIdString.length() - 1);
                stopIdString = stopIdString + "1";
            }
            if(stopIdString.endsWith("S")) {
                stopIdString = stopIdString.substring(0, stopIdString.length() - 1);
                stopIdString = stopIdString + "2";
            }
            if(stopIdString.endsWith("G")) {
                stopIdString = stopIdString.substring(0, stopIdString.length() - 1);
                stopIdString = stopIdString + "3";
            }

            //split at all ":"
            String[] parts = stopIdString.split(":"); // String array, each element is text between dots
            //first two numbers between : will be the csp id
            String cspId = parts[0] + parts[1];
            //all numbers in will be the sp id
            String spId = "";
            for (int i = 0; i < parts.length; i++) {
                spId = spId + parts[i];
            }
            String nameLong = row.getString("stop_name");
            String nameShort;
            //if there is Bstg in the name remove everything after it
            if(nameLong.contains("Bstg")) {
                nameShort = nameLong.substring(0, nameLong.indexOf("Bstg"));
            } else {
                nameShort = nameLong;
            }
            nameShort = nameShort.trim();
            ortRefOrtName.append(nameShort);
            ortRefOrt.append(Double.parseDouble(cspId));
            ortNR.append(Double.parseDouble(spId));
            ortIdOld.append(stopIdStringOld);
        }
        Table locationInfo = Table.create(ortRefOrtName, ortRefOrt, ortNR, ortIdOld);

        //set new csp id int
        double newCSpId = 1;
        for(double cspIdOld : locationInfo.doubleColumn("ORT_REF_ORT")) {
            locationInfo.doubleColumn("ORT_REF_ORT").set(locationInfo.doubleColumn("ORT_REF_ORT").isEqualTo(cspIdOld), newCSpId);
            newCSpId++;
        }
        //set new sp id int
        for(double spIdOld : locationInfo.doubleColumn("ORT_NR")) {
            //you can choose row 0 because ort_nr is unique
            locationInfo.doubleColumn("ORT_NR").set(locationInfo.doubleColumn("ORT_NR").isEqualTo(spIdOld), newCSpId);
            newCSpId++;
        }
        IntColumn intOrtNr = locationInfo.doubleColumn("ORT_NR").asIntColumn();
        IntColumn intOrtRefOrt = locationInfo.doubleColumn("ORT_REF_ORT").asIntColumn();
        locationInfo.removeColumns("ORT_NR");
        locationInfo.removeColumns("ORT_REF_ORT");

        locationInfo.addColumns(intOrtNr, intOrtRefOrt);
        return locationInfo;
    }

    public Table createRideTimetable(Table stopTimesData, Table routeData, Table tripData) {
        IntColumn fromRT = IntColumn.create("ORT_NR");
        IntColumn targetRT = IntColumn.create("SEL_ZIEL");
        IntColumn drivingTimeRT = IntColumn.create("SEL_FZT");
        StringColumn lineNRRT = StringColumn.create("LI_NR");
        IntColumn arrivalRT = IntColumn.create("ARRIVAL");
        IntColumn departureRT = IntColumn.create("DEPARTURE");

        //order by trip_id and stop_sequence -> table where each stop per trip is after the previous stop
        stopTimesData = stopTimesData.sortAscendingOn("trip_id", "stop_sequence");

        int prevTarget = -1;
        int prevDeparture = -1;
        String prevTripId = null;
        for(int i = 0; i < stopTimesData.rowCount(); i++) {
            Row row = stopTimesData.row(i);
            //skip first row of each new tripid
            String tripId = row.getString("trip_id");
            if((prevTarget == -1) || (prevDeparture == -1) || (prevTripId == null) || (!prevTripId.equals(tripId))) {
                prevTripId = row.getString("trip_id");
                prevTarget = row.getInt("ORT_NR");
                prevDeparture = row.getInt("DEPARTURE");
                continue;
            }
            fromRT.append(prevTarget);
            targetRT.append(row.getInt("ORT_NR"));
            drivingTimeRT.append(row.getInt("ARRIVAL") - prevDeparture);
            arrivalRT.append(row.getInt("ARRIVAL"));
            departureRT.append(prevDeparture);
            //index 0 can be selected because every trip can only have one route id (same for line number)
            String routeId = tripData.where(tripData.stringColumn("trip_id").isEqualTo(row.getString("trip_id"))).getString(0, "route_id");
            String lineNR = routeData.where(routeData.stringColumn("route_id").isEqualTo(routeId)).getString(0, "route_short_name");
            lineNRRT.append(lineNR);

            prevTripId = row.getString("trip_id");
            prevTarget = row.getInt("ORT_NR");
            prevDeparture = row.getInt("DEPARTURE");
        }

        return Table.create(fromRT, targetRT, drivingTimeRT, lineNRRT, arrivalRT, departureRT);
    }


    //returns all service ids that do not drive that day and append a '-' so that #1 doesnt remove #11
    public LinkedList<String> getDropServiceIDs(Table calendarData, int day) {
        //get all service_ids that day
        LinkedList<String> sIdThatDay = new LinkedList<>();
        Table calendarDataDay = calendarData.where(calendarData.intColumn("date").isEqualTo(day));
        StringColumn sIDColumnThatDay = calendarDataDay.stringColumn("service_id");
        for(String sID : sIDColumnThatDay) {
            sIdThatDay.add(sID);
        }
        //get all service_ids that do not drive that day
        LinkedList<String> sIdNotThatDay = new LinkedList<>();
        for (String sID : calendarData.stringColumn("service_id").unique()) {
            if(!(sIdThatDay.contains(sID))) {
                sID = sID + '-';
                sIdNotThatDay.add(sID);
            }
        }
        return sIdNotThatDay;
    }

    private void replaceStringPartInColumn(Table table, String columnName, String toReplace, String withReplace) {
        StringColumn renameColumn = table.stringColumn(columnName).replaceAll(toReplace, withReplace);
        table.replaceColumn(columnName, renameColumn);
        table.stringColumn(columnName + "[repl]").setName(columnName);
    }
}
