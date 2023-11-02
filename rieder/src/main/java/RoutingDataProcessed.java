import tech.tablesaw.api.Table;

public class RoutingDataProcessed extends RoutingData{
    private Table rideTimetable;
    private Table rideEvents;
    private Table SPwithTransfer;

    public RoutingDataProcessed(Table locationInfo, Table scheduleData, Table lineCourses, Table drivingTimes, Table rideTimetable, Table rideEvents, Table SPwithTransfer) {
        super(locationInfo, scheduleData, lineCourses, drivingTimes);
        this.rideTimetable = rideTimetable;
        this.rideEvents = rideEvents;
        this.SPwithTransfer = SPwithTransfer;
    }

    public Table getRideTimetable() {
        return rideTimetable;
    }

    public void setRideTimetable(Table rideTimetable) {
        this.rideTimetable = rideTimetable;
    }

    public Table getRideEvents() {
        return rideEvents;
    }

    public void setRideEvents(Table rideEvents) {
        this.rideEvents = rideEvents;
    }

    public Table getSPwithTransfer() {
        return SPwithTransfer;
    }

    public void setSPwithTransfer(Table SPwithTransfer) {
        this.SPwithTransfer = SPwithTransfer;
    }
}
