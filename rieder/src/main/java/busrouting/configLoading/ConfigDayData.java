package busrouting.configLoading;

public class ConfigDayData {
    boolean testData;
    //1-7 for normal data
    //20200504 -> 04.05.2020 for normal data
    int day;

    public ConfigDayData() {
    }

    public boolean isTestData() {
        return testData;
    }

    public void setTestData(boolean testData) {
        this.testData = testData;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }
}
