package busrouting.main.graph;

public class WayPoint {
    private Node node;
    private Node prevNode;
    private boolean isTarget;
    //time at wich a waypoint is reached
    private int time;
    private String reachedBylineNr;
    //if false -> this is reached via transfer
    private boolean isReachedViaRoute;
    private int departureToThisPoint;

    public WayPoint(Node node, Node prevNode, boolean isTarget, int time, String reachedBylineNr, boolean isReachedViaRoute, int departureToThisPoint) {
        this.node = node;
        this.prevNode = prevNode;
        this.isTarget = isTarget;
        this.time = time;
        this.reachedBylineNr = reachedBylineNr;
        this.isReachedViaRoute = isReachedViaRoute;
        this.departureToThisPoint = departureToThisPoint;
    }

    public int getDepartureToThisPoint() {
        return departureToThisPoint;
    }

    public void setDepartureToThisPoint(int departureToThisPoint) {
        this.departureToThisPoint = departureToThisPoint;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public Node getPrevNode() {
        return prevNode;
    }

    public void setPrevNode(Node prevNode) {
        this.prevNode = prevNode;
    }

    public boolean isTarget() {
        return isTarget;
    }

    public void setTarget(boolean target) {
        isTarget = target;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    public boolean isReachedViaRoute() {
        return isReachedViaRoute;
    }

    public void setReachedViaRoute(boolean reachedViaRoute) {
        isReachedViaRoute = reachedViaRoute;
    }

    public String getReachedBylineNr() {
        return reachedBylineNr;
    }

    public void setReachedBylineNr(String reachedBylineNr) {
        this.reachedBylineNr = reachedBylineNr;
    }

    @Override
    public String toString() {
        return node.getIdentifier() + " " + time;
    }

}
