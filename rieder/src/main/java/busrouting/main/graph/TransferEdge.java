package busrouting.main.graph;

public class TransferEdge implements Edge {
    Node toNode;

    public TransferEdge(Node toNode) {
        this.toNode = toNode;
    }

    @Override
    public Node getToNode() {
        return toNode;
    }

    //number of the line -> no line -> transfer
    @Override
    public String getLineNumber() {
        return "transferEdge";
    }

    @Override
    public boolean isRouteEdge() {
        return false;
    }

    //is never only on the next day, because you can transfer on each location every day
    @Override
    public boolean isOnlyNextDay() {
        return false;
    }
}
