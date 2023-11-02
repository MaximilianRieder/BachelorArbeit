package busrouting.main.data;

import tech.tablesaw.api.Table;

public class RoutingDataTestData {
    private Table stopData;
    //will be transferTable
    private Table stopTimesData;
    //get route number from route id
    private Table routeData;
    //get rout id from trip id
    private Table tripData;
    private Table calendarData;

    public RoutingDataTestData(Table stopData, Table stopTimesData, Table routeData, Table tripData, Table calendarData) {
        this.stopData = stopData;
        this.stopTimesData = stopTimesData;
        this.routeData = routeData;
        this.tripData = tripData;
        this.calendarData = calendarData;
    }

    public Table getStopData() {
        return stopData;
    }

    public void setStopData(Table stopData) {
        this.stopData = stopData;
    }

    public Table getStopTimesData() {
        return stopTimesData;
    }

    public void setStopTimesData(Table stopTimesData) {
        this.stopTimesData = stopTimesData;
    }

    public Table getRouteData() {
        return routeData;
    }

    public void setRouteData(Table routeData) {
        this.routeData = routeData;
    }

    public Table getTripData() {
        return tripData;
    }

    public void setTripData(Table tripData) {
        this.tripData = tripData;
    }

    public Table getCalendarData() {
        return calendarData;
    }

    public void setCalendarData(Table calendarData) {
        this.calendarData = calendarData;
    }
}
