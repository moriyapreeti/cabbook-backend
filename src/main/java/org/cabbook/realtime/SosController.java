package org.cabbook.realtime;

import java.security.Principal;
import java.time.Instant;
import org.cabbook.auth.AuthUser;
import org.cabbook.ride.RideRepository;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

@Controller
public class SosController {
    public record SosAlert(Long rideId, Long userId, String role, Instant at) {}

    private final RideRepository rides;
    public SosController(RideRepository rides) { this.rides = rides; }

    /** Client sends to /app/ride/{rideId}/sos  ->  every admin subscribed to /topic/admin/sos gets it. */
    @MessageMapping("/ride/{rideId}/sos")
    @SendTo("/topic/admin/sos")
    public SosAlert sos(@DestinationVariable Long rideId, Principal principal) {
        AuthUser u = (AuthUser) ((UsernamePasswordAuthenticationToken) principal).getPrincipal();
        if (!rides.isParticipant(rideId, u.id())) throw new IllegalArgumentException("Not your ride");
        return new SosAlert(rideId, u.id(), u.role().name(), Instant.now());
    }
}
