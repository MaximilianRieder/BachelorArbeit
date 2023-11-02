import java.util.LinkedList;

public class Graph {
    private RoutingDataProcessed routingDataProcessed;
    private LinkedList<Node> allNodes;
    private LinkedList<Edge> allEdges;

    public Graph(RoutingDataProcessed routingDataProcessed, LinkedList<Node> allNodes, LinkedList<Edge> allEdges) {
        this.routingDataProcessed = routingDataProcessed;
        this.allNodes = allNodes;
        this.allEdges = allEdges;
    }

    //TODO teste ob alle edges nodes stimmen
    /*
    public int getEdgeWeight(Edge edge) {
        if (edge.getFrom() == NodeType.CENTRAL_STOPPING_POINT)
    }*/
}
