import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.selection.Selection;

import static tech.tablesaw.aggregate.AggregateFunctions.*;

import java.io.IOException;
import java.util.List;

public class DataPreProcessor {

    public DataPreProcessor() {
    }

    public RoutingDataProcessed preProcess(RoutingData rawRoutingData) {
        Table locationInfo = rawRoutingData.getLocationInfo();
        Table scheduleData = rawRoutingData.getScheduleData();
        Table lineCourses = rawRoutingData.getLineCourses();
        Table drivingTimes = rawRoutingData.getDrivingTimes();

        //cut not relevant data & remove betriebshöfe & remove teststeige + e ladestationen (index 0 based instead 1 based as in r)
        locationInfo = locationInfo.retainColumns("ONR_TYP_NR", "ORT_NR", "ORT_NAME", "ORT_REF_ORT", "ORT_REF_ORT_KUERZEL", "ORT_REF_ORT_NAME");
        locationInfo = locationInfo.where(locationInfo.intColumn("ONR_TYP_NR").isEqualTo(1));
        locationInfo = locationInfo.dropRows(1, 557, 562, 587, 588);

        //cut not relevant data & only serivce fahrten & remove teststeige
        lineCourses = lineCourses.retainColumns("LI_LFD_NR", "LI_NR", "STR_LI_VAR", "ORT_NR", "PRODUKTIV");
        lineCourses = lineCourses.where(lineCourses.intColumn("PRODUKTIV").isEqualTo(1));
        lineCourses = lineCourses.dropRows(370, 371, 372, 1193, 1194);

        //only mondays & no betriebshof as target & cut relevant data
        scheduleData = scheduleData.where(scheduleData.intColumn("TAGESART_NR").isEqualTo(1));
        scheduleData = scheduleData.where(scheduleData.intColumn("FAHRTART_NR").isEqualTo(1));
        scheduleData = scheduleData.retainColumns("FRT_START", "LI_NR", "FGR_NR", "STR_LI_VAR");

        //no betriebshof as start or target & cut not relevant data
        drivingTimes = drivingTimes.where(drivingTimes.intColumn("ONR_TYP_NR").isEqualTo(1));
        drivingTimes = drivingTimes.where(drivingTimes.intColumn("SEL_ZIEL_TYP").isEqualTo(1));
        drivingTimes = drivingTimes.retainColumns("FGR_NR", "ORT_NR", "SEL_ZIEL", "SEL_FZT");

        //check for not unique ort_name
        /*
        int count = 0;
        for(String k : locationInfo.stringColumn("ORT_NAME")) {
            if(locationInfo.where(locationInfo.stringColumn("ORT_NAME").isEqualTo(k)).intColumn("ORT_REF_ORT").countUnique() > 1)
                count++;
        }
        System.out.println(count);
        */

        //differentiate not unique ort_name
        Selection condition = locationInfo.intColumn("ORT_REF_ORT").isEqualTo(3060);
        locationInfo.stringColumn("ORT_NAME").set(condition, "Keplerstra_e_Neutraubling");
        locationInfo.stringColumn("ORT_NAME").set(locationInfo.intColumn("ORT_REF_ORT").isEqualTo(3044), "Pommernstra_e_Neutraubling");
        locationInfo.stringColumn("ORT_NAME").set(locationInfo.intColumn("ORT_REF_ORT").isEqualTo(3010), "Friedhof_Harting");
        locationInfo.stringColumn("ORT_NAME").set(locationInfo.intColumn("ORT_REF_ORT").isEqualTo(320), "Friedhof_Neutraubling");
        locationInfo.stringColumn("ORT_NAME").set(locationInfo.intColumn("ORT_REF_ORT").isEqualTo(2010), "AussigerStra_e_Dolomitenstra_e");

        //create stopping points with transfer time
        Table temp = locationInfo.select("ORT_REF_ORT", "ORT_NR");
        Table SPwithTransfer = temp.summarize("ORT_NR", count).by("ORT_REF_ORT");
        SPwithTransfer = SPwithTransfer.sortAscendingOn("ORT_REF_ORT");
        SPwithTransfer.doubleColumn(1).setName("FREQ");
        temp = SPwithTransfer.joinOn("ORT_REF_ORT").leftOuter(locationInfo);
        SPwithTransfer = temp.select("ORT_REF_ORT", "FREQ", "ORT_NAME", "ORT_REF_ORT_KUERZEL", "ORT_REF_ORT_NAME").dropDuplicateRows();
        //add transfer time (number of hsp-frequency minus one)
        DoubleColumn transferTime = DoubleColumn.create("TRANSFER_TIME");
        List<Double> frq = SPwithTransfer.doubleColumn("FREQ").asList();
        for (int k = 0; k < frq.size(); k++) {
            transferTime.append(frq.get(k) - 1);
        }
        SPwithTransfer.addColumns(transferTime);
        //exceptions of transfer time
        changeExceptionsTransferTime(SPwithTransfer);

        //create riding timetable (driving times per stopping point)
        Table rideTimetable = drivingTimes.joinOn("FGR_NR").inner(scheduleData);
        //remove all teststations etc. TODO: move this section maybe and more comment
        rideTimetable = rideTimetable.dropWhere(rideTimetable.intColumn("ORT_NR").isEqualTo(700).or(rideTimetable.intColumn("SEL_ZIEL").isEqualTo(700)));
        rideTimetable = rideTimetable.dropWhere(rideTimetable.intColumn("ORT_NR").isEqualTo(800).or(rideTimetable.intColumn("SEL_ZIEL").isEqualTo(800)));
        rideTimetable = rideTimetable.dropWhere(rideTimetable.intColumn("ORT_NR").isEqualTo(900).or(rideTimetable.intColumn("SEL_ZIEL").isEqualTo(900)));
        rideTimetable = rideTimetable.dropWhere(rideTimetable.intColumn("ORT_NR").isEqualTo(1000).or(rideTimetable.intColumn("SEL_ZIEL").isEqualTo(1000)));
        rideTimetable = rideTimetable.dropWhere(rideTimetable.intColumn("ORT_NR").isEqualTo(9200).or(rideTimetable.intColumn("SEL_ZIEL").isEqualTo(9200)));

        //maintain the stable order after sorting
        IntColumn ind = IntColumn.create("INDEX_SORT");
        for (int k = 1; k <= rideTimetable.rowCount(); k++) {
            ind.append(k);
        }
        rideTimetable.addColumns(ind);
        rideTimetable = rideTimetable.sortAscendingOn("FGR_NR", "FRT_START", "INDEX_SORT");
        rideTimetable.removeColumns("INDEX_SORT");
        //fill arival and departure
        IntColumn departureC = IntColumn.create("DEPARTURE");
        IntColumn arrivalC = IntColumn.create("ARRIVAL");
        int startCoursePrev = -1;
        int arrivePrev = 0;
        for (int i = 0; i < rideTimetable.rowCount(); i++) {
            int startCourse = rideTimetable.intColumn("FRT_START").get(i);
            int rideTime = rideTimetable.intColumn("SEL_FZT").get(i);
            //if there is a new starting time -> start at starting time
            if (startCoursePrev != startCourse) {
                departureC.append(startCourse);
                arrivalC.append(startCourse + rideTime);
                arrivePrev = startCourse + rideTime;
            } else { //if starting time is still the same -> calculate driving times
                departureC.append(arrivePrev);
                arrivalC.append(arrivePrev + rideTime);
                arrivePrev = arrivePrev + rideTime;
            }
            startCoursePrev = startCourse;
        }
        rideTimetable.addColumns(departureC, arrivalC);

        //chose the important variables
        Table rideEvents = rideTimetable.retainColumns("ORT_NR", "DEPARTURE", "SEL_ZIEL", "ARRIVAL");
        rideEvents = rideEvents.dropDuplicateRows();

        return new RoutingDataProcessed(locationInfo, scheduleData, lineCourses, drivingTimes, rideTimetable, rideEvents, SPwithTransfer);
    }

    public RoutingData getRawRoutingData() throws IOException {
        Table locationInfo = getTableFromCSV("REC_ORT.csv");
        Table scheduleData = getTableFromCSV("REC_FRT.csv");
        Table lineCourses = getTableFromCSV("LID_VERLAUF.csv");
        Table drivingTimes = getTableFromCSV("SEL_FZT_FELD.csv");

        return new RoutingData(locationInfo, scheduleData, lineCourses, drivingTimes);
    }

    public Table getTableFromCSV(String file) throws IOException {
        CsvReadOptions.Builder builder =
                CsvReadOptions.builder("src/main/resources/" + file)
                        .separator(';')                                        // table is tab-delimited
                        .header(true)                                            // no header
                        .dateFormat("yyyy.MM.dd");                // the date format to use.

        CsvReadOptions options = builder.build();

        Table t1 = Table.read().usingOptions(options);

        return t1;
    }

    //function to cover the exceptions for transfer time
    private Table changeExceptionsTransferTime(Table SPwithTransfer) {
        SPwithTransfer.doubleColumn("TRANSFER_TIME").set(SPwithTransfer.intColumn("ORT_REF_ORT").isEqualTo(336), (double) 3);//        Johann-Hoesl-Str.
        SPwithTransfer.doubleColumn("TRANSFER_TIME").set(SPwithTransfer.intColumn("ORT_REF_ORT").isEqualTo(1010), (double) 3);//        Arnulfsplatz
        SPwithTransfer.doubleColumn("TRANSFER_TIME").set(SPwithTransfer.intColumn("ORT_REF_ORT").isEqualTo(1030), (double) 3);//        Dachauplatz
        SPwithTransfer.doubleColumn("TRANSFER_TIME").set(SPwithTransfer.intColumn("ORT_REF_ORT").isEqualTo(1090), (double) 3);//        Hauptbahnhof
        SPwithTransfer.doubleColumn("TRANSFER_TIME").set(SPwithTransfer.intColumn("ORT_REF_ORT").isEqualTo(1121), (double) 4);//        Alberstr.
        SPwithTransfer.doubleColumn("TRANSFER_TIME").set(SPwithTransfer.intColumn("ORT_REF_ORT").isEqualTo(2001), (double) 2);//        An den Weichser Breiten
        SPwithTransfer.doubleColumn("TRANSFER_TIME").set(SPwithTransfer.intColumn("ORT_REF_ORT").isEqualTo(2021), (double) 2);//        Harzstr.
        SPwithTransfer.doubleColumn("TRANSFER_TIME").set(SPwithTransfer.intColumn("ORT_REF_ORT").isEqualTo(2033), (double) 2);//        Koetztingerstr.
        SPwithTransfer.doubleColumn("TRANSFER_TIME").set(SPwithTransfer.intColumn("ORT_REF_ORT").isEqualTo(2048), (double) 2);//        Reinhausen Kirche
        SPwithTransfer.doubleColumn("TRANSFER_TIME").set(SPwithTransfer.intColumn("ORT_REF_ORT").isEqualTo(2085), (double) 3);//        Weichser Weg
        SPwithTransfer.doubleColumn("TRANSFER_TIME").set(SPwithTransfer.intColumn("ORT_REF_ORT").isEqualTo(2099), (double) 10);//        Siemensstr. /Continental
        SPwithTransfer.doubleColumn("TRANSFER_TIME").set(SPwithTransfer.intColumn("ORT_REF_ORT").isEqualTo(3009), (double) 2);//        Frachtpostzentrum
        SPwithTransfer.doubleColumn("TRANSFER_TIME").set(SPwithTransfer.intColumn("ORT_REF_ORT").isEqualTo(3012), (double) 2);//        Kirche (Harting)
        SPwithTransfer.doubleColumn("TRANSFER_TIME").set(SPwithTransfer.intColumn("ORT_REF_ORT").isEqualTo(3025), (double) 2);//        Prinz-Ludwig-Str.
        SPwithTransfer.doubleColumn("TRANSFER_TIME").set(SPwithTransfer.intColumn("ORT_REF_ORT").isEqualTo(3030), (double) 3);//        Zuckerfabrikstr.
        SPwithTransfer.doubleColumn("TRANSFER_TIME").set(SPwithTransfer.intColumn("ORT_REF_ORT").isEqualTo(4012), (double) 2);//        Brahmsstr.
        SPwithTransfer.doubleColumn("TRANSFER_TIME").set(SPwithTransfer.intColumn("ORT_REF_ORT").isEqualTo(4042), (double) 2);//        Hermann-Geib-Str.
        SPwithTransfer.doubleColumn("TRANSFER_TIME").set(SPwithTransfer.intColumn("ORT_REF_ORT").isEqualTo(4060), (double) 2);//        Leoprechting
        SPwithTransfer.doubleColumn("TRANSFER_TIME").set(SPwithTransfer.intColumn("ORT_REF_ORT").isEqualTo(4061), (double) 2);//        Grass Nord
        SPwithTransfer.doubleColumn("TRANSFER_TIME").set(SPwithTransfer.intColumn("ORT_REF_ORT").isEqualTo(4064), (double) 2);//        Von-Mueller-Gymnasium
        SPwithTransfer.doubleColumn("TRANSFER_TIME").set(SPwithTransfer.intColumn("ORT_REF_ORT").isEqualTo(4069), (double) 2);//        Rauberstr.
        SPwithTransfer.doubleColumn("TRANSFER_TIME").set(SPwithTransfer.intColumn("ORT_REF_ORT").isEqualTo(4071), (double) 3);//        Safferlingstr.
        SPwithTransfer.doubleColumn("TRANSFER_TIME").set(SPwithTransfer.intColumn("ORT_REF_ORT").isEqualTo(4073), (double) 2);//        Antoniuskirche
        SPwithTransfer.doubleColumn("TRANSFER_TIME").set(SPwithTransfer.intColumn("ORT_REF_ORT").isEqualTo(4080), (double) 3);//        Universitaet
        SPwithTransfer.doubleColumn("TRANSFER_TIME").set(SPwithTransfer.intColumn("ORT_REF_ORT").isEqualTo(4082), (double) 2);//        Unterer kath. Friedhof
        SPwithTransfer.doubleColumn("TRANSFER_TIME").set(SPwithTransfer.intColumn("ORT_REF_ORT").isEqualTo(4085), (double) 2);//        Zeissstr.
        SPwithTransfer.doubleColumn("TRANSFER_TIME").set(SPwithTransfer.intColumn("ORT_REF_ORT").isEqualTo(4090), (double) 5);//        Asamstr.
        SPwithTransfer.doubleColumn("TRANSFER_TIME").set(SPwithTransfer.intColumn("ORT_REF_ORT").isEqualTo(4102), (double) 2);//        Hermann-Hoecherl-Str.
        SPwithTransfer.doubleColumn("TRANSFER_TIME").set(SPwithTransfer.intColumn("ORT_REF_ORT").isEqualTo(5001), (double) 2);//        Albertus-Magnus-Gymnasium
        SPwithTransfer.doubleColumn("TRANSFER_TIME").set(SPwithTransfer.intColumn("ORT_REF_ORT").isEqualTo(5016), (double) 2);//        Lessingstr.
        SPwithTransfer.doubleColumn("TRANSFER_TIME").set(SPwithTransfer.intColumn("ORT_REF_ORT").isEqualTo(6006), (double) 2);//        Oberpfalzbruecke
        SPwithTransfer.doubleColumn("TRANSFER_TIME").set(SPwithTransfer.intColumn("ORT_REF_ORT").isEqualTo(6010), (double) 4);//        Pfaffensteiner Bruecke
        SPwithTransfer.doubleColumn("TRANSFER_TIME").set(SPwithTransfer.intColumn("ORT_REF_ORT").isEqualTo(6011), (double) 3);//        Steinweg
        SPwithTransfer.doubleColumn("TRANSFER_TIME").set(SPwithTransfer.intColumn("ORT_REF_ORT").isEqualTo(6012), (double) 2);//        Wuerzburger Str.

        return SPwithTransfer;
    }
}
