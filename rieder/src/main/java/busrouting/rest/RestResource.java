package busrouting.rest;

import busrouting.dto.ShortestWayDTO;


import javax.inject.Inject;
import javax.ws.rs.*;


@Path("shortestway")
public class RestResource {
    @Inject
    public RoutingServiceIF routingService;

    @GET
    @Path("{startId}/{targetId}/{time}")
    @Produces("application/json")
    public ShortestWayDTO makeShortestWay(@PathParam("startId") int startId,
                                       @PathParam("targetId") int targetId,
                                       @PathParam("time") String time) {
        ShortestWayDTO shortestWayDTO = routingService.getShortestWay(startId, targetId, time);

        return shortestWayDTO;
    }
}