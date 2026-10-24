package com.doanltmmt.Backend.config;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.simp.broker.AbstractBrokerMessageHandler;
import org.springframework.stereotype.Component;

/**
 * P4-10 — khi bật relay: kết nối tới RabbitMQ hiện ở {@code /actuator/health} (mục {@code realtimeBroker}) và số liệu
 * {@code taskify_realtime_broker_up}. Không nằm trong nhóm readiness: mất realtime không làm instance bị gỡ khỏi
 * cân bằng tải (REST vẫn chạy; client tự nối lại khi broker trở lại).
 */
@Component
@ConditionalOnProperty(name = "app.realtime.relay.enabled", havingValue = "true")
public class RealtimeBrokerHealthIndicator implements HealthIndicator {

    private final AbstractBrokerMessageHandler relay;

    public RealtimeBrokerHealthIndicator(@Qualifier("stompBrokerRelayMessageHandler") AbstractBrokerMessageHandler relay,
                                         MeterRegistry registry) {
        this.relay = relay;
        Gauge.builder("taskify.realtime.broker.up", relay, r -> r.isBrokerAvailable() ? 1 : 0)
                .description("1 = đang nối tới RabbitMQ (STOMP relay)").register(registry);
    }

    @Override
    public Health health() {
        return relay.isBrokerAvailable() ? Health.up().build() : Health.down().withDetail("reason", "Mất kết nối tới RabbitMQ").build();
    }
}
