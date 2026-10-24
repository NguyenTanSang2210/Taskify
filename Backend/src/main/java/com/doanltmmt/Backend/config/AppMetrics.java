package com.doanltmmt.Backend.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * P4-08 — số liệu riêng của Taskify cho Prometheus (ngoài số liệu JVM/HTTP/Hikari có sẵn):
 * <ul>
 *   <li>{@code taskify_websocket_sessions}: số kết nối STOMP đang mở;</li>
 *   <li>{@code taskify_db_up}: 1 nếu lấy được kết nối CSDL hợp lệ trong 1 giây, ngược lại 0 (dùng cho cảnh báo DB down);</li>
 *   <li>{@code taskify_mail_sent_total{result}}: đếm ở {@code MailDispatcher}.</li>
 * </ul>
 */
@Component
public class AppMetrics {

    private final AtomicInteger websocketSessions = new AtomicInteger();

    public AppMetrics(MeterRegistry registry, DataSource dataSource) {
        Gauge.builder("taskify.websocket.sessions", websocketSessions, AtomicInteger::get)
                .description("Số kết nối WebSocket (STOMP) đang mở").register(registry);
        Gauge.builder("taskify.db.up", dataSource, AppMetrics::databaseUp)
                .description("1 = CSDL trả lời trong 1 giây").register(registry);
    }

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        websocketSessions.incrementAndGet();
    }

    @EventListener
    public void onDisconnected(SessionDisconnectEvent event) {
        websocketSessions.updateAndGet(n -> Math.max(0, n - 1));
    }

    private static double databaseUp(DataSource ds) {
        try (Connection c = ds.getConnection()) {
            return c.isValid(1) ? 1 : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
