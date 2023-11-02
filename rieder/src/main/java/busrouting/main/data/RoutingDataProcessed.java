package busrouting.main.data;

import tech.tablesaw.api.Table;

public class RoutingDataProcessed {
    private Table locationInfo;
    private Table rideTimetable;
    private Table transferTimetable;
    private Table rideTimetableNextDay;

    public RoutingDataProcessed(Table locationInfo, Table rideTimetable, Table transferTimetable, Table rideTimetableNextDay) {
        this.locationInfo = locationInfo;
        this.rideTimetable = rideTimetable;
        this.transferTimetable = transferTimetable;
        this.rideTimetableNextDay = rideTimetableNextDay;
    }

    public Table getRideTimetableNextDay() {
        return rideTimetableNextDay;
    }

    public void setRideTimetableNextDay(Table rideTimetableNextDay) {
        this.rideTimetableNextDay = rideTimetableNextDay;
    }

    public Table getLocationInfo() {
        return locationInfo;
    }

    public void setLocationInfo(Table locationInfo) {
        this.locationInfo = locationInfo;
    }

    public Table getRideTimetable() {
        return rideTimetable;
    }

    public void setRideTimetable(Table rideTimetable) {
        this.rideTimetable = rideTimetable;
    }

    public Table getTransferTimetable() {
        return transferTimetable;
    }

    public void setTransferTimetable(Table transferTimetable) {
        this.transferTimetable = transferTimetable;
    }
}
