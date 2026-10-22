package com.doanltmmt.Backend.security;

import com.doanltmmt.Backend.entity.Topic;
import com.doanltmmt.Backend.entity.User;
import com.doanltmmt.Backend.repository.TopicRepository;
import com.doanltmmt.Backend.repository.UserRepository;
import com.doanltmmt.Backend.service.TopicAccessService;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bảo vệ kênh STOMP (clientInboundChannel):
 * <ul>
 *   <li>{@code CONNECT}: bắt buộc {@code Authorization: Bearer} hợp lệ (scope ACCESS, tài khoản còn bật, tokenVersion khớp).</li>
 *   <li>{@code SUBSCRIBE}: chỉ các đích được liệt kê — kênh riêng {@code /user/queue/*} cho chính người dùng;
 *       {@code /topic/progress/{id}} cho thành viên đề tài; {@code /topic/registration/{id}} cho người quản lý đề tài.
 *       Mọi đích khác bị từ chối.</li>
 *   <li>{@code SEND}: bị từ chối hoàn toàn — client không được tự phát sự kiện (không có {@code @MessageMapping} nào).</li>
 * </ul>
 */
@Component
@SuppressWarnings("null")
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private static final Pattern PROGRESS = Pattern.compile("^/topic/progress/(\\d+)$");
    private static final Pattern REGISTRATION = Pattern.compile("^/topic/registration/(\\d+)$");

    private final TokenAuthenticator tokenAuthenticator;
    private final TopicAccessService topicAccess;
    private final TopicRepository topicRepo;
    private final UserRepository userRepo;

    public WebSocketAuthChannelInterceptor(TokenAuthenticator tokenAuthenticator,
                                           TopicAccessService topicAccess,
                                           TopicRepository topicRepo,
                                           UserRepository userRepo) {
        this.tokenAuthenticator = tokenAuthenticator;
        this.topicAccess = topicAccess;
        this.topicRepo = topicRepo;
        this.userRepo = userRepo;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message; // frame nội bộ / heartbeat
        }
        StompCommand command = accessor.getCommand();
        if (StompCommand.CONNECT.equals(command) || StompCommand.STOMP.equals(command)) {
            authenticate(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            authorizeSubscribe(accessor);
        } else if (StompCommand.SEND.equals(command)) {
            throw new MessagingException("Client không được phép gửi tin lên kênh này.");
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String token = TokenAuthenticator.extractBearer(accessor.getFirstNativeHeader("Authorization"))
                .orElseThrow(() -> new MessagingException("Thiếu token xác thực."));
        // Cùng quy tắc với filter HTTP: đúng scope ACCESS, tài khoản còn bật, tokenVersion khớp
        AuthUser user = tokenAuthenticator.authenticate(token)
                .orElseThrow(() -> new MessagingException("Token không hợp lệ hoặc đã bị thu hồi."));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        user.eraseCredentials();
        accessor.setUser(authentication);
    }

    private void authorizeSubscribe(StompHeaderAccessor accessor) {
        if (!(accessor.getUser() instanceof Authentication auth) || !(auth.getPrincipal() instanceof AuthUser principal)) {
            throw new MessagingException("Chưa xác thực.");
        }
        String dest = accessor.getDestination();
        if (dest == null) {
            throw new MessagingException("Thiếu đích đăng ký.");
        }
        // Kênh riêng: Spring tự định tuyến /user/queue/* theo Principal nên chỉ người đó nhận được.
        if (dest.equals("/user/queue/notifications") || dest.equals("/user/queue/messages")) {
            return;
        }
        Matcher m = PROGRESS.matcher(dest);
        if (m.matches()) {
            Topic topic = loadTopic(m.group(1));
            if (!topicAccess.isMember(topic, loadUser(principal))) {
                throw new MessagingException("Bạn không có quyền theo dõi đề tài này.");
            }
            return;
        }
        m = REGISTRATION.matcher(dest);
        if (m.matches()) {
            Topic topic = loadTopic(m.group(1));
            if (!topicAccess.isOwner(topic, loadUser(principal))) {
                throw new MessagingException("Bạn không có quyền theo dõi đăng ký của đề tài này.");
            }
            return;
        }
        throw new MessagingException("Đích đăng ký không hợp lệ.");
    }

    private Topic loadTopic(String id) {
        return topicRepo.findById(Long.valueOf(id))
                .orElseThrow(() -> new MessagingException("Không tìm thấy đề tài."));
    }

    private User loadUser(AuthUser principal) {
        return userRepo.findById(principal.getId())
                .orElseThrow(() -> new MessagingException("Tài khoản không tồn tại."));
    }
}
