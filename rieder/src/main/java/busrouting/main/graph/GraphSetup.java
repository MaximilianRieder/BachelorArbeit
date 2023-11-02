package busrouting.main.graph;

import busrouting.main.data.RoutingDataProcessed;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.Table;

import java.util.LinkedList;

public class GraphSetup {
    private RoutingDataProcessed routingDataProcessed;
    private boolean displayOnConsole;

    public GraphSetup(RoutingDataProcessed routingDataProcessed) {
        this.routingDataProcessed = routingDataProcessed;
        this.displayOnConsole = false;
    }

    public Graph setup() {
        //generate nodes for graph
        IntColumn centralStoppingPoint = routingDataProcessed.getLocationInfo().intColumn("ORT_REF_ORT").unique();
        IntColumn stoppingPoint = routingDataProcessed.getLocationInfo().intColumn("ORT_NR").unique();
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
        //transfer edges from stoppingPoint to centralBusStop
        linkNodesFromColumns("ORT_NR", "ORT_REF_ORT", routingDataProcessed.getLocationInfo(), allNodes, false);
        //tansfer edges from centralBusStop to stoppingPoint
        linkNodesFromColumns("ORT_REF_ORT", "ORT_NR", routingDataProcessed.getLocationInfo(), allNodes, false);
        //route edges
        linkNodesFromColumns("ORT_NR", "SEL_ZIEL", routingDataProcessed.getRideTimetable(), allNodes, false);
        linkNodesFromColumns("ORT_NR", "SEL_ZIEL", routingDataProcessed.getRideTimetableNextDay(), allNodes, true);
        return new Graph(routingDataProcessed, allNodes, displayOnConsole);
    }

    //get the node with specific identifier
    private Node getNodeByIdentifier(int identifier, LinkedList<Node> nodes){
        for (Node node: nodes) {
            if(node.getIdentifier() == identifier)
                return node;
        }
        return null;
    }
    //link the nodes
    private void linkNodesFromColumns(String fromColumn, String toColumn, Table table, LinkedList<Node>allNodes, boolean onlyNextDay) {
        Table edgeTable = table.select(fromColumn, toColumn);
        for(int i = 0; i < table.rowCount(); i++) {
            Node from = getNodeByIdentifier((int)edgeTable.get(i,0), allNodes);
            Node to = getNodeByIdentifier((int)edgeTable.get(i,1), allNodes);
            Edge edge;
            //if routing edge -> must be generated out of routing data processed -> line nr is in this table
            if((from.getNodeType() == NodeType.STOPPING_POINT) && (to.getNodeType() == NodeType.STOPPING_POINT)) {
                boolean alreadyLinked = false;
                for(Edge edgeFrom : from.getEdges()) {
                    if((edgeFrom.getToNode().equals(to)) && (edgeFrom.getLineNumber().equals(table.stringColumn("LI_NR").getString(i)))) {
                        alreadyLinked = true;
                    }
                }
                if(!alreadyLinked) {
                    String lineNr = table.stringColumn("LI_NR").getString(i);
                    edge = new RouteEdge(to, lineNr, onlyNextDay);
                    from.addEdge(edge);
                }
            } else {
                edge = new TransferEdge(to);
                from.addEdge(edge);
            }
        }
    }

    public void setDisplayOnConsole(boolean displayOnConsole) {
        this.displayOnConsole = displayOnConsole;
    }
}
