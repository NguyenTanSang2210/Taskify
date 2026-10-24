package com.doanltmmt.Backend.security;

import com.doanltmmt.Backend.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.security.Principal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Interceptor WebSocket (không cần mạng): test bắt buộc #15 (CONNECT không token) và #16 (SUBSCRIBE trái phép),
 * cộng SEND, đích lạ, token sai scope / bị thu hồi.
 */
@SuppressWarnings("null")
class WebSocketSecurityIT extends AbstractTopicScenarioIT {

    @Autowired private WebSocketAuthChannelInterceptor interceptor;

    private Message<?> frame(StompHeaderAccessor acc) {
        acc.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], acc.getMessageHeaders());
    }

    private Principal connectAs(User u) {
        StompHeaderAccessor acc = StompHeaderAccessor.create(StompCommand.CONNECT);
        acc.addNativeHeader("Authorization", "Bearer " + tokenOf(u));
        interceptor.preSend(frame(acc), null);
        assertNotNull(acc.getUser(), "CONNECT hợp lệ phải gắn Principal");
        return acc.getUser();
    }

    private void subscribe(Principal who, String destination) {
        StompHeaderAccessor acc = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        acc.setDestination(destination);
        acc.setSubscriptionId("sub-1");
        if (who != null) acc.setUser(who);
        interceptor.preSend(frame(acc), null);
    }

    // ------------------------------------------------------------------ CONNECT (#15)

    @Test
    void connect_withoutToken_isRejected() {
        StompHeaderAccessor acc = StompHeaderAccessor.create(StompCommand.CONNECT);
        assertThrows(MessagingException.class, () -> interceptor.preSend(frame(acc), null));
    }

    @Test
    void connect_withGarbageToken_isRejected() {
        StompHeaderAccessor acc = StompHeaderAccessor.create(StompCommand.CONNECT);
        acc.addNativeHeader("Authorization", "Bearer khong.phai.jwt");
        assertThrows(MessagingException.class, () -> interceptor.preSend(frame(acc), null));
    }

    @Test
    void connect_withPreAuthToken_isRejected() {
        String otpToken = tokens.generatePreAuthToken(userOf(stuA1).getUsername(), userOf(stuA1).getId(), userOf(stuA1).getTokenVersion(), JwtTokenProvider.Scope.OTP_PENDING);
        StompHeaderAccessor acc = StompHeaderAccessor.create(StompCommand.CONNECT);
        acc.addNativeHeader("Authorization", "Bearer " + otpToken);
        assertThrows(MessagingException.class, () -> interceptor.preSend(frame(acc), null));
    }

    @Test
    void connect_afterTokenRevoked_isRejected() {
        User u = userOf(stuA1);
        String old = tokenOf(u);
        u.setTokenVersion(u.getTokenVersion() + 1);
        userRepo.saveAndFlush(u);
        StompHeaderAccessor acc = StompHeaderAccessor.create(StompCommand.CONNECT);
        acc.addNativeHeader("Authorization", "Bearer " + old);
        assertThrows(MessagingException.class, () -> interceptor.preSend(frame(acc), null));
    }

    @Test
    void connect_withValidToken_isAccepted() {
        assertDoesNotThrow(() -> connectAs(userOf(stuA1)));
    }

    // ------------------------------------------------------------------ SUBSCRIBE (#16)

    @Test
    void subscribe_withoutPrincipal_isRejected() {
        assertThrows(MessagingException.class, () -> subscribe(null, "/topic/notifications." + userOf(stuA1).getId()));
    }

    @Test
    void subscribe_personalChannels_onlyForTheOwner() {
        Principal p = connectAs(userOf(stuA1));
        long own = userOf(stuA1).getId();
        long other = userOf(stuA2).getId();
        assertDoesNotThrow(() -> subscribe(p, "/topic/notifications." + own));
        assertDoesNotThrow(() -> subscribe(p, "/topic/messages." + own));
        assertThrows(MessagingException.class, () -> subscribe(p, "/topic/notifications." + other));
        assertThrows(MessagingException.class, () -> subscribe(p, "/topic/messages." + other));
        // Admin cũng không đọc trộm kênh riêng của người khác
        Principal a = connectAs(admin);
        assertThrows(MessagingException.class, () -> subscribe(a, "/topic/notifications." + own));
    }

    @Test
    void subscribe_oldPerUserTopics_areRejected() {
        Principal p = connectAs(userOf(stuA1));
        long id = userOf(stuA1).getId();
        assertThrows(MessagingException.class, () -> subscribe(p, "/topic/notifications/" + id));
        assertThrows(MessagingException.class, () -> subscribe(p, "/topic/messages/" + id));
        assertThrows(MessagingException.class, () -> subscribe(p, "/topic/events"));
        // kênh riêng kiểu cũ của Spring (danh sách phiên theo từng instance) không còn dùng
        assertThrows(MessagingException.class, () -> subscribe(p, "/user/queue/notifications"));
        assertThrows(MessagingException.class, () -> subscribe(p, "/user/queue/messages"));
    }

    @Test
    void subscribe_progress_onlyForTopicMembers() {
        String dest = "/topic/progress." + topicA.getId();
        for (User allowed : new User[]{userOf(stuA1), userOf(lecA), deptAdminA, admin}) {
            Principal p = connectAs(allowed);
            assertDoesNotThrow(() -> subscribe(p, dest), allowed.getUsername());
        }
        for (User denied : new User[]{userOf(stuA2), userOf(stuB1), userOf(lecB), deptAdminB}) {
            Principal p = connectAs(denied);
            assertThrows(MessagingException.class, () -> subscribe(p, dest), denied.getUsername());
        }
    }

    @Test
    void subscribe_registration_onlyForTopicOwners_notStudents() {
        String dest = "/topic/registration." + topicA.getId();
        for (User allowed : new User[]{userOf(lecA), deptAdminA, admin}) {
            Principal p = connectAs(allowed);
            assertDoesNotThrow(() -> subscribe(p, dest), allowed.getUsername());
        }
        for (User denied : new User[]{userOf(stuA1), userOf(stuA2), userOf(lecB), deptAdminB}) {
            Principal p = connectAs(denied);
            assertThrows(MessagingException.class, () -> subscribe(p, dest), denied.getUsername());
        }
    }

    @Test
    void subscribe_unknownTopic_andUnknownDestinations_areRejected() {
        Principal p = connectAs(admin);
        assertThrows(MessagingException.class, () -> subscribe(p, "/topic/progress.99999999"));
        assertThrows(MessagingException.class, () -> subscribe(p, "/topic/progress.abc"));
        assertThrows(MessagingException.class, () -> subscribe(p, "/topic/whatever"));
        assertThrows(MessagingException.class, () -> subscribe(p, "/queue/anything"));
    }

    // ------------------------------------------------------------------ SEND (chặn client tự phát sự kiện)

    @Test
    void send_fromClient_isAlwaysRejected() {
        Principal p = connectAs(admin);
        StompHeaderAccessor acc = StompHeaderAccessor.create(StompCommand.SEND);
        acc.setDestination("/topic/progress." + topicA.getId());
        acc.setUser(p);
        assertThrows(MessagingException.class, () -> interceptor.preSend(frame(acc), null));
    }

    @Test
    void otherFrames_passThrough() {
        StompHeaderAccessor acc = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        assertDoesNotThrow(() -> interceptor.preSend(frame(acc), null));
    }
}
