package busrouting.dto;

import java.util.LinkedList;
import java.util.List;

public class ShortestWayDTO {
    LinkedList<RoutingPart> routingParts = new LinkedList<>();

    public ShortestWayDTO() {
    }

    public List<RoutingPart> getRoutingParts() {
        return routingParts;
    }


    public void addRoutingPart(RoutingPart rp) {
        routingParts.add(rp);
    }
}
