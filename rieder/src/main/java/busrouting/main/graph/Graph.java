package busrouting.main.graph;

import busrouting.main.data.DataTransformer;
import busrouting.main.data.RoutingDataProcessed;
import tech.tablesaw.api.Table;
import tech.tablesaw.selection.Selection;

import java.util.*;

public class Graph {
    final private RoutingDataProcessed routingDataProcessed;
    final private ArrayList<Node> nodes;
    final private boolean displayOnConsole;

    public Graph(RoutingDataProcessed routingDataProcessed, LinkedList<Node> nodes, boolean displayOnConsole) {
        this.routingDataProcessed = routingDataProcessed;
        this.nodes = new ArrayList<>(nodes);
        this.displayOnConsole = displayOnConsole;
    }

    //dijkstra
    public Way shortestWay(int fromIdentifier, int toIdentifier, int startTime) {
        Way way = new Way();
        //costs for transfer in time stamps (low arrival time -> low cost)
        int timeStamps[] = new int[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            timeStamps[i] = Integer.MAX_VALUE;
        }
        LinkedList<Node> closedNodes = new LinkedList<>();
        LinkedList<Node> openNodes = new LinkedList<>();
        //first node starting point
        Node startNode = getNodeByIdentifier(fromIdentifier, nodes);
        openNodes.add(startNode);
        timeStamps[nodes.indexOf(startNode)] = startTime;
        way.addPoint(startNode, null, false, startTime, "transferEdge", false, -1);

        while (!openNodes.isEmpty()) {
            Node currentNode = getNodeWithLowestCost(openNodes, timeStamps);
            openNodes.remove(currentNode);

            //just for display
            if(displayOnConsole) {
                DataTransformer dt = new DataTransformer(routingDataProcessed.getLocationInfo());
                if(!(currentNode.getIdentifier() == fromIdentifier)) {
                    int timeDisplay = timeStamps[nodes.indexOf(currentNode)];
                    if(timeDisplay > 86400) {
                        timeDisplay = timeDisplay - 86400;
                    }
                    System.out.println("shortest way is currently to " + dt.getNameByIdentifier(currentNode.getIdentifier()) + " (" + currentNode.getNodeType().toString() + ")" + " at " + dt.makeTimeFromSeconds(timeDisplay));
                    System.out.println("    Locations that can be reached at an earlier time than before: ");
                }
            }

            if(currentNode.getIdentifier() == toIdentifier) {
                WayPoint wayPoint = way.getWayPointFromNode(currentNode);
                wayPoint.setTarget(true);
                break;
            }
            LinkedList<Edge> edgesCurrentNode = currentNode.getEdges();
            for(Edge edge : edgesCurrentNode) {
                Node adjacentNode = edge.getToNode();
                int arrivalTime;
                if(!closedNodes.contains(adjacentNode)){
                    TransportTime transportTime = getTransportTime(currentNode, adjacentNode, edge.getLineNumber(), timeStamps[nodes.indexOf(currentNode)], way, edge.isOnlyNextDay());
                    if(currentNode.getIdentifier() == fromIdentifier) {
                        //no transfer time at starting location
                        arrivalTime = timeStamps[nodes.indexOf(currentNode)];
                    } else {
                        int duration = transportTime.getDuration();
                        arrivalTime =  duration + timeStamps[nodes.indexOf(currentNode)];
                    }
                    //if new better time
                    if(timeStamps[nodes.indexOf(adjacentNode)] > arrivalTime) {
                        openNodes.add(adjacentNode);
                        timeStamps[nodes.indexOf(adjacentNode)] = arrivalTime;
                        //new point reached
                        way.addPoint(adjacentNode, currentNode, false, arrivalTime, edge.getLineNumber(), edge.isRouteEdge(), transportTime.getDeparture());

                        //just for printing
                        if(displayOnConsole) {
                            DataTransformer dt = new DataTransformer(routingDataProcessed.getLocationInfo());
                            int timeDisplay = arrivalTime;
                            if(timeDisplay > 86400) {
                                timeDisplay = timeDisplay - 86400;
                            }
                            if (!(currentNode.getIdentifier() == fromIdentifier)) {
                                if (currentNode.getNodeType() == NodeType.STOPPING_POINT) {
                                    if (edge.isRouteEdge()) {
                                        System.out.println("        " + dt.getNameByIdentifier(edge.getToNode().getIdentifier()) + " with line " + edge.getLineNumber() + " will make you arrive at " + dt.makeTimeFromSeconds(timeDisplay));
                                    } else {
                                        System.out.println("        " + NodeType.CENTRAL_STOPPING_POINT.toString() + " of " + dt.getNameByIdentifier(currentNode.getIdentifier()) + " with no time delay");
                                    }
                                } else {
                                    System.out.println("        " + NodeType.STOPPING_POINT.toString() + " of " + dt.getNameByIdentifier(currentNode.getIdentifier()) + " (Transfer to this point takes until: " + dt.makeTimeFromSeconds(timeDisplay) + ")");
                                }
                            }
                        }

                    }
                }
            }
            closedNodes.add(currentNode);
        }
        return way;
    }

    //returns edge weight as Transport Time and the arrival time
    private TransportTime getTransportTime(Node from, Node to, String lineNr, int atTime, Way way, boolean onlyNextDay) {
        //the edgecost to travel to a csp are 0 and the costs are calculated when you move to the next sp
        //departure -> immediately
        if ((from.getNodeType() == NodeType.STOPPING_POINT) & (to.getNodeType() == NodeType.CENTRAL_STOPPING_POINT))
            return new TransportTime(0, atTime);
        //read the costs to move from csp to sp from the data frame (* 60 to convert minutes into seconds)
        //departure -> immediately
        if ((from.getNodeType() == NodeType.CENTRAL_STOPPING_POINT) & (to.getNodeType() == NodeType.STOPPING_POINT)) {
            Table transferTimetable = routingDataProcessed.getTransferTimetable();
            Table transferForCSP = transferTimetable.where(transferTimetable.intColumn("ORT_REF_ORT").isEqualTo(from.getIdentifier()));
            int duration =  transferForCSP.row(0).getInt("TRANSFER_TIME") * 60;
            return new TransportTime(duration, atTime);
        }
        //calculate the dynamic cost depending on time
        if ((from.getNodeType() == NodeType.STOPPING_POINT) & (to.getNodeType() == NodeType.STOPPING_POINT)) {
            Table rideTimetable = routingDataProcessed.getRideTimetable();
            Selection whereFrom = rideTimetable.intColumn("ORT_NR").isEqualTo(from.getIdentifier());
            Selection whereTo = rideTimetable.intColumn("SEL_ZIEL").isEqualTo(to.getIdentifier());
            Table edgeTimetable = rideTimetable.where(whereFrom.and(whereTo));
            edgeTimetable = edgeTimetable.where(edgeTimetable.stringColumn("LI_NR").isEqualTo(lineNr));

            int latestDeparture = (int) edgeTimetable.intColumn("DEPARTURE").max();
            String prevLineNr = way.getWayPointFromNode(from).getReachedBylineNr();

            //there is a edge for this day and the edge is not only for the next day
            if ((latestDeparture >= atTime) && !(onlyNextDay)) {
                edgeTimetable = edgeTimetable.where(edgeTimetable.intColumn("DEPARTURE").isGreaterThanOrEqualTo(atTime));
                edgeTimetable = getEdgeTimetableWithOnlyEarliestArrival(edgeTimetable, prevLineNr);
                int duration = edgeTimetable.row(0).getInt("ARRIVAL") - atTime;
                int departure = edgeTimetable.row(0).getInt("DEPARTURE");
                return new TransportTime(duration, departure);
            } else { //no edge for this day -> look at next day
                Table rideTimetableNextDay = routingDataProcessed.getRideTimetableNextDay();
                Selection whereFromNextDay = rideTimetableNextDay.intColumn("ORT_NR").isEqualTo(from.getIdentifier());
                Selection whereToNextDay = rideTimetableNextDay.intColumn("SEL_ZIEL").isEqualTo(to.getIdentifier());
                Table edgeTimetableNextDay = rideTimetableNextDay.where(whereFromNextDay.and(whereToNextDay));
                edgeTimetableNextDay = edgeTimetableNextDay.where(edgeTimetableNextDay.stringColumn("LI_NR").isEqualTo(lineNr));

                //if there is no connection on the next day return Interger.MaxValue / 2 so it wont be chosen again
                if(edgeTimetableNextDay.rowCount() == 0) {
                    return new TransportTime((Integer.MAX_VALUE) / 2, (Integer.MAX_VALUE) / 2);
                }

                //if atTime is greater than 86400 -> adjust seconds for next day
                if(atTime > 86400) {
                    atTime = atTime - 86400;
                    edgeTimetableNextDay = edgeTimetableNextDay.where(edgeTimetableNextDay.intColumn("DEPARTURE").isGreaterThanOrEqualTo(atTime));
                    //if there is no connection on the next day return Interger.MaxValue / 2 so it wont be chosen again
                    if(edgeTimetableNextDay.rowCount() == 0) {
                        return new TransportTime((Integer.MAX_VALUE) / 2, (Integer.MAX_VALUE) / 2);
                    }
                    edgeTimetableNextDay = getEdgeTimetableWithOnlyEarliestArrival(edgeTimetableNextDay, prevLineNr);
                    int duration = edgeTimetableNextDay.row(0).getInt("ARRIVAL") - atTime;
                    int departure = 86400 + edgeTimetableNextDay.row(0).getInt("DEPARTURE");
                    return new TransportTime(duration, departure);
                } else {//atTime is on the first day -> you can choose the earliest possible weight
                    edgeTimetableNextDay = getEdgeTimetableWithOnlyEarliestArrival(edgeTimetableNextDay, prevLineNr);
                    int duration = 86400 + edgeTimetableNextDay.row(0).getInt("ARRIVAL") - atTime;
                    int departure = 86400 + edgeTimetableNextDay.row(0).getInt("DEPARTURE");
                    return new TransportTime(duration, departure);
                }
            }
        }
        return new TransportTime(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }
    private Table getEdgeTimetableWithOnlyEarliestArrival(Table edgeTimetable, String prevLineNr) {
        double earliestArrival = edgeTimetable.intColumn("ARRIVAL").min();
        edgeTimetable = edgeTimetable.where(edgeTimetable.intColumn("ARRIVAL").isEqualTo(earliestArrival));
        return edgeTimetable;
    }

    //acts as priority queue
    private Node getNodeWithLowestCost(List<Node> inNodes, int[] timestamps){
        Node lowestCostNode = null;
        int lowestCost = Integer.MAX_VALUE;
        for (Node node: inNodes) {
            int cost = timestamps[nodes.indexOf(node)];
            if (cost < lowestCost) {
                lowestCost = cost;
                lowestCostNode = node;
            }
        }
        return lowestCostNode;
    }
    private Node getNodeByIdentifier(int identifier, Collection<Node> nodes){
        for (Node node: nodes) {
            if(node.getIdentifier() == identifier)
                return node;
        }
        return null;
    }
}
