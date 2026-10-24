package com.doanltmmt.Backend.security;

import com.doanltmmt.Backend.config.RequestIdFilter;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** P3-08 — mã yêu cầu X-Request-Id và Actuator health/info. */
class ObservabilityIT extends AbstractSecurityIT {

    @Test
    void everyResponseCarriesARequestId_andErrorsEchoIt() throws Exception {
        mvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(RequestIdFilter.HEADER, matchesPattern("[a-f0-9]{16}")));
        mvc.perform(get("/api/khong-ton-tai").header(AUTH, bearer(newUser("aobs", "ADMIN", null)))
                        .header(RequestIdFilter.HEADER, "abc-123"))
                .andExpect(status().isNotFound())
                .andExpect(header().string(RequestIdFilter.HEADER, "abc-123"))
                .andExpect(jsonPath("$.requestId").value("abc-123"));
    }

    @Test
    void unsafeIncomingRequestIdIsReplaced() throws Exception {
        mvc.perform(get("/actuator/health/liveness").header(RequestIdFilter.HEADER, "x\r\nGIẢ DÒNG LOG"))
                .andExpect(header().string(RequestIdFilter.HEADER, matchesPattern("[a-f0-9]{16}")));
    }

    @Test
    void healthProbesArePublic_detailsAreNot() throws Exception {
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        // không đăng nhập: chỉ có trạng thái tổng, không lộ chi tiết CSDL/email
        mvc.perform(get("/actuator/health")).andExpect(jsonPath("$.status").exists()).andExpect(jsonPath("$.components").doesNotExist());
        mvc.perform(get("/actuator/health").header(AUTH, bearer(newUser("aobs2", "ADMIN", null))))
                .andExpect(jsonPath("$.components.db.status").value("UP"));
        mvc.perform(get("/actuator/info")).andExpect(status().isOk()).andExpect(jsonPath("$.app.name").value("Taskify"));
    }

    @Test
    void otherActuatorEndpointsAreNotExposed() throws Exception {
        String admin = bearer(newUser("aobs3", "ADMIN", null));
        for (String path : new String[]{"/actuator/env", "/actuator/beans", "/actuator/heapdump", "/actuator/loggers"}) {
            mvc.perform(get(path).header(AUTH, admin)).andExpect(status().isNotFound());
        }
        mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
    }
}
