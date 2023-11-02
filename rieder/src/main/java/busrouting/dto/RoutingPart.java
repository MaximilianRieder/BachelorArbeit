package busrouting.dto;

public class RoutingPart {
    int fromId;
    int toId;
    int fromCSP;
    int toCSP;
    String fromName;
    String toName;
    String lineNr;
    String departureTime;
    String arrivalTime;
    boolean nextDay;

    public RoutingPart(int fromId, int toId, int fromCSP, int toCSP, String fromName, String toName, String lineNr, String departureTime, String arrivalTime, boolean nextDay) {
        this.fromId = fromId;
        this.toId = toId;
        this.fromCSP = fromCSP;
        this.toCSP = toCSP;
        this.fromName = fromName;
        this.toName = toName;
        this.lineNr = lineNr;
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.nextDay = nextDay;
    }

    public String getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(String arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public boolean isNextDay() {
        return nextDay;
    }

    public void setNextDay(boolean nextDay) {
        this.nextDay = nextDay;
    }

    public int getFromId() {
        return fromId;
    }

    public void setFromId(int fromId) {
        this.fromId = fromId;
    }

    public int getToId() {
        return toId;
    }

    public void setToId(int toId) {
        this.toId = toId;
    }

    public int getFromCSP() {
        return fromCSP;
    }

    public void setFromCSP(int fromCSP) {
        this.fromCSP = fromCSP;
    }

    public int getToCSP() {
        return toCSP;
    }

    public void setToCSP(int toCSP) {
        this.toCSP = toCSP;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public String getToName() {
        return toName;
    }

    public void setToName(String toName) {
        this.toName = toName;
    }

    public String getLineNr() {
        return lineNr;
    }

    public void setLineNr(String lineNr) {
        this.lineNr = lineNr;
    }

    public String getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(String departureTime) {
        this.departureTime = departureTime;
    }
}
