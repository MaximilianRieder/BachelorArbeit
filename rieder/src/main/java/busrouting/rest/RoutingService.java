package busrouting.rest;

import busrouting.configLoading.ConfigDayData;
import busrouting.configLoading.ConfigDayDataLoader;
import busrouting.dto.RoutingPart;
import busrouting.dto.ShortestWayDTO;
import busrouting.main.data.*;
import busrouting.main.graph.Graph;
import busrouting.main.graph.GraphSetup;
import busrouting.main.graph.Way;
import busrouting.main.graph.WayPoint;

import javax.inject.Singleton;
import java.io.IOException;
import java.util.LinkedList;

public class RoutingService implements RoutingServiceIF {
    Graph graph;
    RoutingDataProcessed routingDataProcessed;

    public RoutingService() throws IOException {
        ConfigDayDataLoader configDayDataLoader = new ConfigDayDataLoader("configDayData.yaml");
        ConfigDayData configDayData = configDayDataLoader.getConfigDayData();
        DataInOut dataInOut = new DataInOut(configDayData.isTestData());
        routingDataProcessed = dataInOut.getRoutingDataProcessed(configDayData.getDay());
        GraphSetup graphSetup = new GraphSetup(routingDataProcessed);
        this.graph = graphSetup.setup();
    }

    public ShortestWayDTO getShortestWay(int fromId, int toId, String time) {
        ShortestWayDTO shortestWayDTO = new ShortestWayDTO();
        DataTransformer dataTransformer = new DataTransformer(routingDataProcessed.getLocationInfo());
        Way way = graph.shortestWay(fromId, toId, dataTransformer.makeSecondsFromTime(time));

        LinkedList<WayPoint> wayList =  way.getShortestWayPointsInOrder();

        WayPoint prevWayPoint = null;
        for(WayPoint wayPoint : wayList) {
            if(prevWayPoint == null) {
            } else {
                int fId = prevWayPoint.getNode().getIdentifier();
                int tId = wayPoint.getNode().getIdentifier();
                String fName = dataTransformer.getNameByIdentifier(fId);
                String tName = dataTransformer.getNameByIdentifier(tId);
                int fCSP = dataTransformer.getCSPIdentifierByName(fName);
                int tCSP = dataTransformer.getCSPIdentifierByName(tName);
                String lNr = wayPoint.getReachedBylineNr();
                RoutingPart routingPart;
                int wayPointDepartureTo = wayPoint.getDepartureToThisPoint();
                int wayPointArrival = wayPoint.getTime();
                if (wayPointArrival > 86400)
                    wayPointArrival = wayPointArrival - 86400;
                if(wayPointDepartureTo > 86400) {
                    wayPointDepartureTo = wayPointDepartureTo - 86400;
                    String td = dataTransformer.makeTimeFromSeconds(wayPointDepartureTo);
                    String ta = dataTransformer.makeTimeFromSeconds(wayPointArrival);
                    routingPart = new RoutingPart(fId, tId, fCSP, tCSP, fName, tName, lNr, td, ta, true);
                } else {
                    String td = dataTransformer.makeTimeFromSeconds(wayPointDepartureTo);
                    String ta = dataTransformer.makeTimeFromSeconds(wayPointArrival);
                    routingPart = new RoutingPart(fId, tId, fCSP, tCSP, fName, tName, lNr, td, ta,false);
                }
                shortestWayDTO.addRoutingPart(routingPart);
            }
            prevWayPoint = wayPoint;
        }

        return shortestWayDTO;
    }
}
