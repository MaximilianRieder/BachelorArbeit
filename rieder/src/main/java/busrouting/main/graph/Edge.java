package busrouting.main.graph;

public interface Edge {
    public Node getToNode();
    public String getLineNumber();
    public boolean isRouteEdge();
    public boolean isOnlyNextDay();
}
