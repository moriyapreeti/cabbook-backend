package org.cabbook.pricing;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "cabbook.routing.provider", havingValue = "haversine", matchIfMissing = true)
public class HaversineRouteProvider implements RouteProvider {
    private static final double ROAD_FACTOR = 1.3;   // roads are longer than a straight line
    private final double avgSpeedKmph;

    public HaversineRouteProvider(@Value("${cabbook.fare.avg-speed-kmph}") double avgSpeedKmph) {
        this.avgSpeedKmph = avgSpeedKmph;
    }

    @Override
    public RouteInfo route(double fromLat, double fromLng, double toLat, double toLng) {
        double km = GeoUtils.haversineKm(fromLat, fromLng, toLat, toLng) * ROAD_FACTOR;
        int minutes = (int) Math.max(1, Math.round(km / avgSpeedKmph * 60));
        return new RouteInfo(Math.round(km * 100.0) / 100.0, minutes);
    }
}
