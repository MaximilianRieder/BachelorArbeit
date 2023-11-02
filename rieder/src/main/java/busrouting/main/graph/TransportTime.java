package busrouting.main.graph;

public class TransportTime {
    int duration;
    int departure;

    public TransportTime(int duration, int departure) {
        this.duration = duration;
        this.departure = departure;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getDeparture() {
        return departure;
    }

    public void setDeparture(int departure) {
        this.departure = departure;
    }
}
