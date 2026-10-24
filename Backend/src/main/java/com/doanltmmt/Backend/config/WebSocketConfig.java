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

/**
 * STOMP qua SockJS tại {@code /ws}. Đích dùng dấu chấm sau tiền tố ({@code /topic/progress.5}) để chạy được cả với
 * broker trong bộ nhớ lẫn RabbitMQ (RabbitMQ không nhận {@code /} trong tên đích).
 *
 * <p>P4-10: {@code app.realtime.relay.enabled=true} → chuyển tiếp tới RabbitMQ (plugin STOMP) để mọi instance backend
 * cùng nhận sự kiện. Kênh riêng là {@code /topic/notifications.{userId}}, {@code /topic/messages.{userId}} (chỉ chủ
 * kênh được đăng ký) thay vì {@code /user/queue/*}: broker phát tới mọi tab của người đó trên mọi instance, không phụ
 * thuộc danh sách phiên mà Spring đồng bộ giữa các instance mỗi 10 giây (dễ gửi vào phiên đã đóng sau khi tải lại trang).
 */
@Configuration
@EnableWebSocketMessageBroker
@SuppressWarnings("null")
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthChannelInterceptor wsAuthInterceptor;
    private final String[] allowedOrigins;
    private final Relay relay;

    /** Kết nối tới RabbitMQ; {@code login/passcode} dùng cho cả phiên hệ thống và phiên của từng client. */
    record Relay(boolean enabled, String host, int port, String login, String passcode, String virtualHost) {
    }

    public WebSocketConfig(WebSocketAuthChannelInterceptor wsAuthInterceptor,
                           @Value("${app.cors.allowed-origins:http://localhost:5175}") String allowedOrigins,
                           @Value("${app.realtime.relay.enabled:false}") boolean relayEnabled,
                           @Value("${app.realtime.relay.host:rabbitmq}") String relayHost,
                           @Value("${app.realtime.relay.port:61613}") int relayPort,
                           @Value("${app.realtime.relay.login:taskify}") String relayLogin,
                           @Value("${app.realtime.relay.passcode:}") String relayPasscode,
                           @Value("${app.realtime.relay.virtual-host:/}") String relayVirtualHost) {
        this.wsAuthInterceptor = wsAuthInterceptor;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toArray(String[]::new);
        this.relay = new Relay(relayEnabled, relayHost, relayPort, relayLogin, relayPasscode, relayVirtualHost);
        if (relayEnabled && relayPasscode.isBlank()) {
            throw new IllegalStateException("APP_REALTIME_RELAY_PASSCODE là bắt buộc khi bật APP_REALTIME_RELAY_ENABLED.");
        }
    }

    @Override
    public void configureMessageBroker(@NonNull MessageBrokerRegistry config) {
        if (relay.enabled()) {
            config.enableStompBrokerRelay("/topic", "/queue")
                    .setRelayHost(relay.host())
                    .setRelayPort(relay.port())
                    .setSystemLogin(relay.login())
                    .setSystemPasscode(relay.passcode())
                    .setClientLogin(relay.login())
                    .setClientPasscode(relay.passcode())
                    .setVirtualHost(relay.virtualHost());
        } else {
            config.enableSimpleBroker("/topic", "/queue");
        }
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
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
