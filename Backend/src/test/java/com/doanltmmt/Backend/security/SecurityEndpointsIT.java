package com.doanltmmt.Backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Mặc định TỪ CHỐI: duyệt MỌI endpoint đã đăng ký trong ứng dụng và khẳng định rằng, khi không có token,
 * chỉ đúng danh sách công khai mới không trả 401. Thêm endpoint mới mà quên bảo vệ ⇒ test này đỏ.
 */
@SuppressWarnings("null")
class SecurityEndpointsIT extends AbstractSecurityIT {

    /** Danh sách công khai DUY NHẤT được phép (POST). Khớp SecurityConfig.PUBLIC_AUTH_ENDPOINTS. */
    private static final Set<String> PUBLIC_POST = Set.of(
            "/api/auth/login",
            "/api/auth/otp/verify",
            "/api/auth/otp/resend",
            "/api/auth/activate",
            "/api/auth/password/initial",
            "/api/auth/password/forgot",   // P2-05
            "/api/auth/password/reset");   // P2-05

    @Autowired private RequestMappingHandlerMapping mapping;

    @Test
    void everyRegisteredEndpoint_requiresAuthentication_exceptTheExplicitPublicList() throws Exception {
        List<String> openEndpoints = new ArrayList<>();
        int checked = 0;

        for (Map.Entry<RequestMappingInfo, HandlerMethod> e : mapping.getHandlerMethods().entrySet()) {
            if (!e.getValue().getBeanType().getPackageName().startsWith("com.doanltmmt")) continue; // bỏ /error, springdoc...
            Set<String> patterns = e.getKey().getPathPatternsCondition().getPatternValues();
            Set<RequestMethod> methods = e.getKey().getMethodsCondition().getMethods();
            List<HttpMethod> httpMethods = new ArrayList<>();
            if (methods.isEmpty()) httpMethods.add(HttpMethod.GET);
            methods.forEach(m -> httpMethods.add(HttpMethod.valueOf(m.name())));

            for (String pattern : patterns) {
                String url = pattern.replaceAll("\\{[^/}]+}", "1");
                for (HttpMethod m : httpMethods) {
                    checked++;
                    int s = mvc.perform(request(m, url)).andReturn().getResponse().getStatus();
                    boolean isPublic = m == HttpMethod.POST && PUBLIC_POST.contains(url);
                    if (isPublic) {
                        assertTrue(s != 401 && s != 403, "Endpoint công khai bị chặn nhầm: " + m + " " + url + " → " + s);
                    } else if (s != 401) {
                        openEndpoints.add(m + " " + url + " → " + s);
                    }
                }
            }
        }
        assertTrue(checked > 50, "Số endpoint duyệt được bất thường: " + checked);
        assertEquals(List.of(), openEndpoints, "Các endpoint KHÔNG yêu cầu đăng nhập ngoài danh sách công khai");
    }

    @Test
    void unauthenticatedAndGarbageTokens_get401_not403() throws Exception {
        mvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users").header(AUTH, "Bearer khong.phai.jwt")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users").header(AUTH, "Basic abc")).andExpect(status().isUnauthorized());
    }

    @Test
    void removedEndpoints_areGone() throws Exception {
        var admin = newUser("adm", "ADMIN", null);
        mvc.perform(get("/api/users/test").header(AUTH, bearer(admin))).andExpect(status().is4xxClientError());
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/register")
                .header(AUTH, bearer(admin))).andExpect(status().isNotFound());
    }

    @Test
    void swagger_isOpenInDevProfile() throws Exception {
        // Profile mặc định khi chạy test là dev ⇒ app.swagger.public=true
        int s = mvc.perform(get("/swagger-ui.html")).andReturn().getResponse().getStatus();
        assertTrue(s != 401 && s != 403, "Swagger phải mở được ở dev, nhận " + s);
    }
}
