package busrouting.rest;
import busrouting.dto.ShortestWayDTO;

public interface RoutingServiceIF {
    public ShortestWayDTO getShortestWay(int fromId, int toId, String time);
}
