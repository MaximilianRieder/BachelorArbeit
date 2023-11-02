package busrouting.main.data;

import tech.tablesaw.api.Table;

public class RoutingDataStandardData {
    private Table locationInfo; //REC_ORT
    private Table scheduleData; //REC_FRT
    private Table drivingTimes; //SEL_FZT_FELD


    public RoutingDataStandardData(Table locationInfo, Table scheduleData, Table drivingTimes) {
        this.locationInfo = locationInfo;
        this.scheduleData = scheduleData;
        this.drivingTimes = drivingTimes;
    }


    public Table getLocationInfo() {
        return locationInfo;
    }

    public void setLocationInfo(Table locationInfo) {
        this.locationInfo = locationInfo;
    }

    public Table getScheduleData() {
        return scheduleData;
    }

    public void setScheduleData(Table scheduleData) {
        this.scheduleData = scheduleData;
    }

    public Table getDrivingTimes() {
        return drivingTimes;
    }

    public void setDrivingTimes(Table drivingTimes) {
        this.drivingTimes = drivingTimes;
    }
}
