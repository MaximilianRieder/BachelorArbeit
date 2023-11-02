import java.util.LinkedList;

public class Node {
    private int identifyer;
    private NodeType nodeType;
    private LinkedList<Node> adjecentNodes;

    public Node(int identifyer, NodeType nodeType) {
        this.identifyer = identifyer;
        this.nodeType = nodeType;
        this.adjecentNodes = new LinkedList<>();
    }

    public int getIdentifyer() {
        return identifyer;
    }

    public void setIdentifyer(int identifyer) {
        this.identifyer = identifyer;
    }

    public NodeType getNodeType() {
        return nodeType;
    }

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    public LinkedList<Node> getAdjecentNodes() {
        return adjecentNodes;
    }

    public void setAdjecentNodes(LinkedList<Node> adjecentNodes) {
        this.adjecentNodes = adjecentNodes;
    }

    public void addAdjecentNode(Node node) {
        this.adjecentNodes.add(node);
    }
}
