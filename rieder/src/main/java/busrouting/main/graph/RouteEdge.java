package busrouting.main.graph;

public class RouteEdge implements Edge {
    Node toNode;
    String lineNumber;
    boolean onlyNextDay;

    public RouteEdge(Node toNode, String lineNumber, boolean onlyNextDay) {
        this.toNode = toNode;
        this.lineNumber = lineNumber;
        this.onlyNextDay = onlyNextDay;
    }

    @Override
    public Node getToNode() {
        return toNode;
    }

    @Override
    public String getLineNumber() {
        return lineNumber;
    }

    @Override
    public boolean isRouteEdge() {
        return true;
    }

    @Override
    public boolean isOnlyNextDay() {
        return onlyNextDay;
    }
}
