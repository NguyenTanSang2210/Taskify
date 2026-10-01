package com.doanltmmt.Backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Production (app.swagger.public=false): tài liệu API không truy cập được nếu không đăng nhập. */
@TestPropertySource(properties = "app.swagger.public=false")
class SwaggerClosedIT extends AbstractSecurityIT {

    @Test
    void swagger_isNotPubliclyReachable() throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().isUnauthorized());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api-docs")).andExpect(status().isUnauthorized());
    }
}
