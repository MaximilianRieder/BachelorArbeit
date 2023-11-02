package busrouting.rest;

import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import javax.inject.Singleton;

public class JerseyConfiguration extends ResourceConfig {

    public JerseyConfiguration() {
        register(RestResource.class);
        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(RoutingService.class).to(RoutingServiceIF.class).in(Singleton.class);
            }
        });
        register(JacksonFeature.class);
    }
}