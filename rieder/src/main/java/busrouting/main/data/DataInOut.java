package busrouting.main.data;

import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.csv.CsvReadOptions;
import tech.tablesaw.io.csv.CsvWriteOptions;
import tech.tablesaw.io.csv.CsvWriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class DataInOut {
    boolean useTestData;
    public DataInOut(boolean useTestData) {
        this.useTestData = useTestData;
    }

    /*will return routing data processed form file if available
        *if not preprocess it from data files
     */
    public RoutingDataProcessed getRoutingDataProcessed(int day) throws IOException {
        if(checkRoutingDataProcessedExistence()){
            return loadRoutingDataProcessedFromFile();
        } else {
            return makeRoutingDataPreprocessedFromData(day);
        }
    }

    //generates RoutingDataPreprocessed from files and update the RoutingDataPreprocessed files
    public RoutingDataProcessed makeRoutingDataPreprocessedFromData(int day) throws IOException {
        RoutingDataProcessed routingDataProcessed;
        if(useTestData) {
            RoutingDataTestData routingDataTestData = getRawRoutingDataTestData();
            DataPreProcessorTestData dataPreProcessorTestData = new DataPreProcessorTestData();
            routingDataProcessed = dataPreProcessorTestData.preProcess(routingDataTestData, day);
        } else {
            RoutingDataStandardData rawRoutingDataStandardData = getRawRoutingDataStandardData();
            DataPreProcessorStandardData dataPreProcessorStandardData = new DataPreProcessorStandardData();
            routingDataProcessed = dataPreProcessorStandardData.preProcess(rawRoutingDataStandardData, day);
        }
        writeRoutingDataProcessedToCSV(routingDataProcessed);
        return routingDataProcessed;
    }

    public RoutingDataProcessed loadRoutingDataProcessedFromFile() throws IOException {
        String preString = getPreString();
        Table rideTimetable = getTableFromCSV(preString + "RoutingDataProcessed" + File.separator + "rideTimetable" + preString + ".csv", ';');
        Table rideTimetableNextDay = getTableFromCSV(preString + "RoutingDataProcessed" + File.separator + "rideTimetableNextDay" + preString + ".csv", ';');
        Table locationInfo = getTableFromCSV(preString + "RoutingDataProcessed" + File.separator + "locationInfo" + preString + ".csv", ';');
        Table transferTimetable = getTableFromCSV(preString + "RoutingDataProcessed" + File.separator + "transferTimetable" + preString + ".csv", ';');

        rideTimetable = rideTimetable.replaceColumn("LI_NR", rideTimetable.column("LI_NR").asStringColumn());
        rideTimetableNextDay = rideTimetableNextDay.replaceColumn("LI_NR", rideTimetableNextDay.column("LI_NR").asStringColumn());

        if(rideTimetable.columnNames().contains("LI_NR strings")) {
            rideTimetable.stringColumn("LI_NR strings").setName("LI_NR");
        }
        if(rideTimetableNextDay.columnNames().contains("LI_NR strings")) {
            rideTimetableNextDay.stringColumn("LI_NR strings").setName("LI_NR");
        }

        return new RoutingDataProcessed(locationInfo, rideTimetable, transferTimetable, rideTimetableNextDay);
    }

    public boolean checkRoutingDataProcessedExistence() {
        String preString = getPreString();
        File dir = new File(preString + "RoutingDataProcessed");
        File rideTimetable = new File(preString + "RoutingDataProcessed" + File.separator + "rideTimetable" + preString + ".csv");
        File rideTimetableNextDay = new File(preString + "RoutingDataProcessed" + File.separator + "rideTimetableNextDay" + preString + ".csv");
        File transferTimetable = new File(preString + "RoutingDataProcessed" + File.separator + "transferTimetable" + preString + ".csv");
        File locationInfo = new File(preString + "RoutingDataProcessed" + File.separator + "locationInfo" + preString + ".csv");
        if (dir.exists() && rideTimetable.exists() && rideTimetableNextDay.exists() && transferTimetable.exists() && locationInfo.exists()) {
            return true;
        } else {
            return false;
        }
    }

    public void writeRoutingDataProcessedToCSV(RoutingDataProcessed routingDataProcessed) throws IOException {
        String preString = getPreString();
        new File(preString + "RoutingDataProcessed").mkdirs();
        writeTableToCSV(routingDataProcessed.getRideTimetable(), preString + "RoutingDataProcessed" + File.separator + "rideTimetable" + preString + ".csv", ';');
        writeTableToCSV(routingDataProcessed.getRideTimetableNextDay(), preString + "RoutingDataProcessed" + File.separator + "rideTimetableNextDay" + preString + ".csv", ';');
        writeTableToCSV(routingDataProcessed.getTransferTimetable(), preString + "RoutingDataProcessed" + File.separator +"transferTimetable" + preString + ".csv", ';');
        writeTableToCSV(routingDataProcessed.getLocationInfo(), preString + "RoutingDataProcessed" + File.separator + "locationInfo" + preString + ".csv", ';');
    }

    public void writeTableToCSV(Table table, String file, char separator) throws IOException {
        CsvWriteOptions.Builder builder =
                CsvWriteOptions.builder(file)
                        .separator(separator)                                        // table is tab-delimited
                        .header(true);                                           // no header

        CsvWriteOptions options = builder.build();

        CsvWriter csvWriter = new CsvWriter();
        csvWriter.write(table, options);
    }

    public void writeNameIdentFile(Table locationInfo, String fileName) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, false));
        StringColumn nameColumn = locationInfo.stringColumn("ORT_REF_ORT_NAME").unique();
        nameColumn.sortDescending();
        for(String name : nameColumn) {
            //chose 0 because all ort_ref_ort_name have the same ident
            String identifier = locationInfo.where(locationInfo.stringColumn("ORT_REF_ORT_NAME").isEqualTo(name)).getString(0, "ORT_REF_ORT");
            writer.append(name);
            writer.append(" ");
            writer.append(identifier);
            writer.append(System.lineSeparator());
        }
        writer.close();
    }

    //get standard vdv data
    public RoutingDataStandardData getRawRoutingDataStandardData() throws IOException {
        Table locationInfo = getTableFromCSV("REC_ORT.csv", ';');
        Table scheduleData = getTableFromCSV("REC_FRT.csv", ';');
        Table drivingTimes = getTableFromCSV("SEL_FZT_FELD.csv", ';');

        return new RoutingDataStandardData(locationInfo, scheduleData, drivingTimes);
    }

    //get testdata
    public RoutingDataTestData getRawRoutingDataTestData() throws IOException {
        //naming etc.
        Table stopInfo = getTableFromCSV("stops.csv", ',');
        //will be used for rideTimetable
        Table stopTimes = getTableFromCSV("stop_times.csv", ',');
        //get route number from route id
        Table routeInfo = getTableFromCSV("routes.csv", ',');
        //get rout id from trip id
        Table tripInfo = getTableFromCSV("trips.csv", ',');
        //info when which lanes drive
        Table calendar = getTableFromCSV("calendar_dates.csv", ',');

        return new RoutingDataTestData(stopInfo, stopTimes, routeInfo, tripInfo, calendar);
    }

    public Table getTableFromCSV(String file, char separator) throws IOException {
        CsvReadOptions.Builder builder =
                CsvReadOptions.builder(file)
                        .separator(separator)
                        .header(true)
                        .dateFormat("yyyy.MM.dd");

        CsvReadOptions options = builder.build();

        Table t1 = Table.read().usingOptions(options);

        return t1;
    }

    private String getPreString() {
        if(useTestData) {
            return "TestData";
        } else {
            return  "StandardData";
        }
    }
}
