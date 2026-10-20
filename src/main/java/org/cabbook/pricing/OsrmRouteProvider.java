package org.cabbook.pricing;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Uses a self-hosted OSRM server (https://project-osrm.org) with OpenStreetMap data. */
@Component
@ConditionalOnProperty(name = "cabbook.routing.provider", havingValue = "osrm")
public class OsrmRouteProvider implements RouteProvider {
    private final RestClient client;

    public OsrmRouteProvider(@Value("${cabbook.routing.osrm-url}") String baseUrl) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public RouteInfo route(double fromLat, double fromLng, double toLat, double toLng) {
        JsonNode body = client.get()
                .uri("/route/v1/driving/{a},{b};{c},{d}?overview=false", fromLng, fromLat, toLng, toLat)
                .retrieve().body(JsonNode.class);
        JsonNode r = body.path("routes").path(0);
        if (r.isMissingNode()) throw new IllegalStateException("OSRM returned no route");
        double km = r.path("distance").asDouble() / 1000.0;
        int min = (int) Math.max(1, Math.round(r.path("duration").asDouble() / 60.0));
        return new RouteInfo(Math.round(km * 100.0) / 100.0, min);
    }
}
