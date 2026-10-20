package org.cabbook.pricing;

/** Pluggable routing. Default = straight-line estimate; OSRM (open source) gives real road distance and ETA. */
public interface RouteProvider {
    RouteInfo route(double fromLat, double fromLng, double toLat, double toLng);
}
