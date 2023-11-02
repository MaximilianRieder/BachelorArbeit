package busrouting.main.graph;

import java.util.LinkedList;

public class Node {
    private int identifier;
    private NodeType nodeType;
    private LinkedList<Edge> edges;

    public Node(int identifier, NodeType nodeType) {
        this.identifier = identifier;
        this.nodeType = nodeType;
        this.edges = new LinkedList<>();
    }

    public int getIdentifier() {
        return identifier;
    }

    public void setIdentifier(int identifier) {
        this.identifier = identifier;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    public LinkedList<Edge> getEdges() {
        return edges;
    }

    public void setEdges(LinkedList<Edge> edges) {
        this.edges = edges;
    }

    public void addEdge(Edge edge) {
        this.edges.add(edge);
    }
}
