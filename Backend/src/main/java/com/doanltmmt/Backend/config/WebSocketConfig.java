package com.doanltmmt.Backend.config;

import com.doanltmmt.Backend.security.WebSocketAuthChannelInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.lang.NonNull;

import java.util.Arrays;

@Configuration
@EnableWebSocketMessageBroker
@SuppressWarnings("null")
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthChannelInterceptor wsAuthInterceptor;
    private final String[] allowedOrigins;

    public WebSocketConfig(WebSocketAuthChannelInterceptor wsAuthInterceptor,
                           @Value("${app.cors.allowed-origins:http://localhost:5173}") String allowedOrigins) {
        this.wsAuthInterceptor = wsAuthInterceptor;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toArray(String[]::new);
    }

    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(@NonNull StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins)
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(@NonNull ChannelRegistration registration) {
        registration.interceptors(wsAuthInterceptor);
    }
}
