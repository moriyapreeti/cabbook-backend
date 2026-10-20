package org.cabbook.realtime;

import java.util.Map;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/** All WebSocket pushes go through here so topic names live in one place. */
@Component
public class RealtimePublisher {
    private final SimpMessagingTemplate template;
    public RealtimePublisher(SimpMessagingTemplate template) { this.template = template; }

    /** Rider + driver of this ride subscribe to /topic/ride/{id} */
    public void rideStatus(Long rideId, String status, String message) {
        template.convertAndSend("/topic/ride/" + rideId, Map.of("rideId", rideId, "status", status, "message", message));
    }

    /** Live driver marker: /topic/ride/{id}/location */
    public void driverLocation(Long rideId, double lat, double lng) {
        template.convertAndSend("/topic/ride/" + rideId + "/location", Map.of("lat", lat, "lng", lng));
    }

    /** New ride request pushed to one driver (by user id): /topic/driver/{userId}/requests */
    public void rideRequest(Long driverUserId, Object ride) {
        template.convertAndSend("/topic/driver/" + driverUserId + "/requests", ride);
    }

    /** Tell other drivers a request is gone (accepted/cancelled). */
    public void rideRequestClosed(Long driverUserId, Long rideId) {
        template.convertAndSend("/topic/driver/" + driverUserId + "/requests", Map.of("rideId", rideId, "closed", true));
    }
}
