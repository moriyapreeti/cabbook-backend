package org.cabbook.realtime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final WebSocketAuthInterceptor interceptor;
    private final String[] origins;

    public WebSocketConfig(WebSocketAuthInterceptor interceptor,
                           @Value("${cabbook.cors.allowed-origins}") String origins) {
        this.interceptor = interceptor;
        this.origins = origins.split(",");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry r) {
        r.addEndpoint("/ws").setAllowedOrigins(origins);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry r) {
        r.enableSimpleBroker("/topic");          // swap for RabbitMQ/Redis relay when running multiple instances
        r.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration r) {
        r.interceptors(interceptor);
    }
}
