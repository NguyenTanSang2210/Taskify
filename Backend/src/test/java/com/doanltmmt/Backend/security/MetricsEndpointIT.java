package com.doanltmmt.Backend.security;

import com.doanltmmt.Backend.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.test.context.TestPropertySource;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** P4-08 — /actuator/prometheus chỉ mở với header X-Metrics-Token đúng; có số liệu riêng của Taskify. */
// Spring Boot tắt export số liệu trong test theo mặc định — bật lại để kiểm tra endpoint thật
@AutoConfigureObservability
@TestPropertySource(properties = "app.metrics.token=token-kiem-thu-metrics")
class MetricsEndpointIT extends AbstractSecurityIT {

    @Test
    void withTheToken_exposesPrometheusMetrics() throws Exception {
        mvc.perform(get("/api/auth/me")); // tạo ít nhất một số liệu http.server.requests
        mvc.perform(get("/actuator/prometheus").header(MetricsTokenAuthorization.HEADER, "token-kiem-thu-metrics"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("taskify_db_up{application=\"taskify-backend\"} 1.0")))
                .andExpect(content().string(containsString("taskify_websocket_sessions")))
                .andExpect(content().string(containsString("http_server_requests_seconds_bucket")))
                .andExpect(content().string(containsString("jvm_memory_used_bytes")));
    }

    @Test
    void withoutOrWithAWrongToken_isRejected_evenForAnAdmin() throws Exception {
        mvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/prometheus").header(MetricsTokenAuthorization.HEADER, "sai")).andExpect(status().isUnauthorized());
        User admin = newUser("adm", "ADMIN", null);
        mvc.perform(get("/actuator/prometheus").header(AUTH, bearer(admin))).andExpect(status().isForbidden());
    }
}
