import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.Table;

import java.util.LinkedList;
import java.util.List;

public class GraphSetup {
    private RoutingDataProcessed routingDataProcessed;

    public GraphSetup(RoutingDataProcessed routingDataProcessed) {
        this.routingDataProcessed = routingDataProcessed;
    }

    public Graph setup() {

        //TODO: unique doesnt keep the order -> is order needed?
        //generate nodes for graph
        IntColumn centralStoppingPoint = routingDataProcessed.getLocationInfo().intColumn("ORT_REF_ORT").unique();
        IntColumn stoppingPoint = routingDataProcessed.getLineCourses().intColumn("ORT_NR").unique();
        LinkedList<Node> CSPNodes = new LinkedList<>();
        LinkedList<Node> SPNodes = new LinkedList<>();
        for(int i = 0; i < centralStoppingPoint.size(); i++) {
            Node node = new Node(centralStoppingPoint.get(i), NodeType.CENTRAL_STOPPING_POINT);
            CSPNodes.add(node);
        }
        for(int i = 0; i < stoppingPoint.size(); i++) {
            Node node = new Node(stoppingPoint.get(i), NodeType.STOPPING_POINT);
            SPNodes.add(node);
        }
        //all nodes
        LinkedList<Node> allNodes = new LinkedList<>();
        allNodes.addAll(CSPNodes);
        allNodes.addAll(SPNodes);
        //transfer edges from stoppingPoint to centralBusStop -> select for safety(no hard code)
        //TODO: create makeEdge("from","to")
        //TODO: check earlier if all identifyers are unique
        LinkedList<Edge> CtoPEdges = linkNodesFromColumns("ORT_NR", "ORT_REF_ORT", routingDataProcessed.getLocationInfo(), allNodes);
        //tansfer edges from centralBusStop to stoppingPoint
        LinkedList<Edge> PtoCEdges = linkNodesFromColumns("ORT_REF_ORT", "ORT_NR", routingDataProcessed.getLocationInfo(), allNodes);
        //route edges
        LinkedList<Edge> routeEdges = linkNodesFromColumns("ORT_NR", "SEL_ZIEL", routingDataProcessed.getRideEvents(), allNodes);
        //all edges
        //TODO: in masterarbeit wird bei A und D -> A[-3] verwendet?
        LinkedList<Edge> allEdges = new LinkedList<>();
        allEdges.addAll(CtoPEdges);
        allEdges.addAll(PtoCEdges);
        allEdges.addAll(routeEdges);
        //TODO r_allg steht hier noch(keine praktische relevanz aktuell)
        return new Graph(routingDataProcessed, allNodes, allEdges);
    }

    //get the node with specific identifyer
    public Node getNodeByIdentifyer(int identifyer, LinkedList<Node> nodes){
        for (Node node: nodes) {
            if(node.getIdentifyer() == identifyer)
                return node;
        }
        return null;
    }
    //get edges from two columns in a dataframe
    public LinkedList<Edge> linkNodesFromColumns(String fromColumn, String toColumn, Table table, LinkedList<Node>allNodes) {
        LinkedList<Edge> edges = new LinkedList<>();
        Table edgeTable = table.select(fromColumn, toColumn);
        for(int i = 0; i < table.rowCount(); i++) {
            //generate edge -> return them at the end
            Node from = getNodeByIdentifyer((int)edgeTable.get(i,0), allNodes);
            Node to = getNodeByIdentifyer((int)edgeTable.get(i,1), allNodes);
            Edge edge = new Edge(from, to);
            edges.add(edge);
            //add the to-node to the adjecent list of the from-node -> link
            from.addAdjecentNode(to);
            System.out.println(i);
        }
        return edges;
    }
}
