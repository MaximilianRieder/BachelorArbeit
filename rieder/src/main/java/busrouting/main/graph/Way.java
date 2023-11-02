package busrouting.main.graph;

import java.util.LinkedList;

public class Way {
    LinkedList<WayPoint> points;

    public Way() {
        this.points = new LinkedList<>();
    }

    //adds a point if there is no waypoint with this node already
    public void addPoint(Node node, Node prevNode, boolean isTarget, int time, String reachedByLineNr, boolean isReachedViaRoute, int departureToThisPoint) {
        //to secure that there are not more than one waypoints with the same node
        if(getWayPointFromNode(node) != null) {
            WayPoint wayPoint = getWayPointFromNode(node);
            wayPoint.setTime(time);
            wayPoint.setPrevNode(prevNode);
            wayPoint.setTarget(isTarget);
            wayPoint.setReachedBylineNr(reachedByLineNr);
            wayPoint.setReachedViaRoute(isReachedViaRoute);
            wayPoint.setDepartureToThisPoint(departureToThisPoint);
        } else {
            points.add(new WayPoint(node, prevNode, isTarget, time, reachedByLineNr, isReachedViaRoute, departureToThisPoint));
        }
    }
    public WayPoint getWayPointFromNode(Node node) {
        for(WayPoint wayPoint : points) {
            if (wayPoint.getNode() == node)
                return wayPoint;
        }
        return null;
    }
    public WayPoint getTarget(){
        for (WayPoint wayPoint : points) {
            if (wayPoint.isTarget())
                return wayPoint;
        }
        return null;
    }

    public LinkedList<WayPoint> getPoints() {
        return points;
    }

    public void setPoints(LinkedList<WayPoint> points) {
        this.points = points;
    }

    public LinkedList<WayPoint> getShortestWayPointsInOrder() {
        LinkedList<WayPoint> wayList = new LinkedList<>();
        WayPoint wayPoint = getTarget();
        while (wayPoint != null) {
            wayList.addFirst(wayPoint);
            Node node = wayPoint.getPrevNode();
            wayPoint = getWayPointFromNode(node);
        }
        return wayList;
    }
}
