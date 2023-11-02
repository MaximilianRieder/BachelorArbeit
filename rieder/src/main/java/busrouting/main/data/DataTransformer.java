package busrouting.main.data;

import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

public class DataTransformer {
    Table locationInfo;
//    Table nameIdentifierTable;

    public DataTransformer(Table locationInfo) {
        this.locationInfo = locationInfo;
//        this.nameIdentifierTable = makeNameIdentifierTable(locationInfo);
    }

    //returns the central stopping point of the location with name
    public int getCSPIdentifierByName(String name){
        Table identifierLocation = locationInfo.where(locationInfo.stringColumn("ORT_REF_ORT_NAME").isEqualTo(name));
        return identifierLocation.intColumn("ORT_REF_ORT").get(0);
    }

    public String getNameByIdentifier(int identifier) {
        Table nameSpIdent;
        Table nameCspIdent;
        nameCspIdent = locationInfo.where(locationInfo.intColumn("ORT_REF_ORT").isEqualTo(identifier));
        if(nameCspIdent.rowCount() == 0) {
            nameSpIdent = locationInfo.where(locationInfo.intColumn("ORT_NR").isEqualTo(identifier));
            return nameSpIdent.stringColumn("ORT_REF_ORT_NAME").get(0);
        } else {
            return nameCspIdent.stringColumn("ORT_REF_ORT_NAME").get(0);
        }
    }

    //makes a new Table with names and identifiers
//    private Table makeNameIdentifierTable(Table locationInfo) {
//        Table identifierNameTable = Table.create(IntColumn.create("IDENTIFIER"), StringColumn.create("NAME"));
//        for (Row row : locationInfo) {
//            String name = row.getString("ORT_REF_ORT_NAME");
//            int spIdent = row.getInt("ORT_NR");
//            int cspIdent = row.getInt("ORT_REF_ORT");
//            int[] identifiers = {spIdent, cspIdent};
//            String[] names = {name, name};
//            Table appendTable = Table.create(IntColumn.create("IDENTIFIER", identifiers), StringColumn.create("NAME", names));
//            identifierNameTable.append(appendTable);
//        }
//        identifierNameTable = identifierNameTable.dropDuplicateRows();
//        return identifierNameTable;
//    }

    public String makeTimeFromSeconds(int time) {
        int seconds;
        int  minutes;
        int hours;
        seconds = time % 60;
        time = time / 60;
        minutes = time % 60;
        time = time / 60;
        hours = time;
        return hours + ":" + minutes + ":" + seconds;
    }

    //accepts time in format hh:mm:ss
    public int makeSecondsFromTime(String time) {
        String[] parts = time.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        int seconds = Integer.parseInt(parts[2]);

        return (hours * 60 * 60) + (minutes * 60) + seconds;
    }
}