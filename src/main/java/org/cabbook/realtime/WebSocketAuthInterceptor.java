package org.cabbook.realtime;

import java.util.List;
import org.cabbook.auth.AuthUser;
import org.cabbook.auth.JwtService;
import org.cabbook.ride.RideRepository;
import org.cabbook.user.Role;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * CONNECT: client must send header "Authorization: Bearer <jwt>".
 * SUBSCRIBE: users may only listen to their own rides / their own driver queue; admin topics need ADMIN.
 */
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    private final JwtService jwt;
    private final RideRepository rides;

    public WebSocketAuthInterceptor(JwtService jwt, RideRepository rides) { this.jwt = jwt; this.rides = rides; }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor acc = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (acc == null || acc.getCommand() == null) return message;
        StompCommand cmd = acc.getCommand();

        if (cmd == StompCommand.CONNECT) {
            String h = acc.getFirstNativeHeader("Authorization");
            AuthUser u = (h != null && h.startsWith("Bearer ")) ? jwt.parse(h.substring(7)) : null;
            if (u == null) throw new MessagingException("Unauthorized");
            acc.setUser(new UsernamePasswordAuthenticationToken(u, null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + u.role().name()))));
            return message;
        }

        if (cmd == StompCommand.SUBSCRIBE) {
            AuthUser u = currentUser(acc);
            String dest = acc.getDestination() == null ? "" : acc.getDestination();
            if (!allowed(u, dest)) throw new MessagingException("Forbidden subscription: " + dest);
        } else if (cmd == StompCommand.SEND) {
            if (currentUser(acc) == null) throw new MessagingException("Unauthorized");
        }
        return message;
    }

    private boolean allowed(AuthUser u, String dest) {
        if (u == null) return false;
        if (dest.startsWith("/topic/admin/")) return u.role() == Role.ADMIN;
        if (dest.startsWith("/topic/driver/")) {
            String[] p = dest.split("/");                // ["", "topic", "driver", "{userId}", "requests"]
            return p.length >= 4 && p[3].equals(String.valueOf(u.id()));
        }
        if (dest.startsWith("/topic/ride/")) {
            String[] p = dest.split("/");                // ["", "topic", "ride", "{rideId}", ...]
            try {
                return u.role() == Role.ADMIN || rides.isParticipant(Long.valueOf(p[3]), u.id());
            } catch (RuntimeException e) { return false; }
        }
        return false;
    }

    static AuthUser currentUser(StompHeaderAccessor acc) {
        if (acc.getUser() instanceof UsernamePasswordAuthenticationToken t && t.getPrincipal() instanceof AuthUser a) return a;
        return null;
    }
}
