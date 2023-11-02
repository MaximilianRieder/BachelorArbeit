package busrouting.main.data;

import tech.tablesaw.api.Table;

import java.util.LinkedList;

public class ConditionChecker {
    public ConditionChecker() {
    }

    public void checkFIFO(Table rideTimetable, Table locationInfo) {
        DataTransformer dataTransformer = new DataTransformer(locationInfo);
        Table fifoTimetable = rideTimetable.copy();

        for(Integer stoppingPoint : fifoTimetable.intColumn("ORT_NR").unique()) {
            for(Integer target : fifoTimetable.intColumn("SEL_ZIEL").unique()) {
                Table fifoCompare = fifoTimetable.where(fifoTimetable.intColumn("ORT_NR").isEqualTo(stoppingPoint).and(fifoTimetable.intColumn("SEL_ZIEL").isEqualTo(target)));
                for(int i = 0; i < fifoCompare.rowCount(); i++) {
                    for(int j = i+1; j < fifoCompare.rowCount(); j++) {
                        int jDeparture = Integer.parseInt(fifoCompare.getString(j,"DEPARTURE"));
                        int iDeparture = Integer.parseInt(fifoCompare.getString(i,"DEPARTURE"));
                        int iArrival = Integer.parseInt(fifoCompare.getString(i,"ARRIVAL"));
                        int jArrival = Integer.parseInt(fifoCompare.getString(j,"ARRIVAL"));
                        if(jDeparture > iArrival)
                            break;
                        if((iDeparture < jDeparture) && (iArrival >= jArrival)) {
                            if(fifoCompare.getString(i,"LI_NR").equals(fifoCompare.getString(j,"LI_NR"))) {
                                System.out.println("<-------------------------------!!-------------------------->");
                                System.out.println("FIFO condition Violated -> Algorithm may not work as intended");
                                System.out.println("Please check if the Files are correct");
                                System.out.println("Line " + fifoCompare.getString(i,"LI_NR") + " drives at " + dataTransformer.makeTimeFromSeconds(Integer.parseInt(fifoCompare.getString(i,"DEPARTURE"))) + " and arrives at " + dataTransformer.makeTimeFromSeconds(Integer.parseInt(fifoCompare.getString(i,"ARRIVAL"))));
                                System.out.println("Line " + fifoCompare.getString(j,"LI_NR") + " drives at " + dataTransformer.makeTimeFromSeconds(Integer.parseInt(fifoCompare.getString(j,"DEPARTURE"))) + " and arrives at " + dataTransformer.makeTimeFromSeconds(Integer.parseInt(fifoCompare.getString(j,"ARRIVAL"))));
                                System.out.println("<-------------------------------!!-------------------------->");
                            }
                        }
                    }
                }
            }
        }
    }

    public void checkUniqueLocationNames(Table locationInfo) {
        LinkedList<String> alreadyChecked = new LinkedList<>();
        System.out.println("Rename the duplicate location Names for the same identifier by writing them into the fileException.yaml:");
        for(String k : locationInfo.stringColumn("ORT_REF_ORT_NAME")) {
            if(locationInfo.where(locationInfo.stringColumn("ORT_REF_ORT_NAME").isEqualTo(k)).intColumn("ORT_REF_ORT").countUnique() > 1) {
                if(alreadyChecked.contains(k))
                    continue;
                System.out.println(locationInfo.where(locationInfo.stringColumn("ORT_REF_ORT_NAME").isEqualTo(k)).printAll());
                alreadyChecked.add(k);
            }
        }
    }
}
