package com.doanltmmt.Backend.security;

import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final TokenAuthenticator tokenAuthenticator;

    public WebSocketAuthChannelInterceptor(TokenAuthenticator tokenAuthenticator) {
        this.tokenAuthenticator = tokenAuthenticator;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            Optional<String> token = TokenAuthenticator.extractBearer(accessor.getFirstNativeHeader("Authorization"));
            if (token.isPresent()) {
                // Cùng quy tắc với filter HTTP: đúng scope ACCESS, tài khoản còn bật, tokenVersion khớp
                AuthUser user = tokenAuthenticator.authenticate(token.get())
                        .orElseThrow(() -> new IllegalArgumentException("Invalid JWT token"));
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                user.eraseCredentials();
                accessor.setUser(authentication);
            }
        }
        return message;
    }
}
