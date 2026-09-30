package com.doanltmmt.Backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P1-10 — giới hạn tần suất THEO IP ở ngưỡng production (10 lần đăng nhập/phút). Profile dev mặc định nới lỏng
 * lên 120 nên lớp này ghi đè ngưỡng (tạo một Spring context riêng).
 */
@TestPropertySource(properties = {
        "app.security.rate.login-per-minute=10",
        "app.security.rate.otp-verify-per-minute=3",
        "app.security.rate.otp-resend-per-10-minutes=2"
})
@SuppressWarnings("null")
class RateLimitIT extends AbstractSecurityIT {

    private static RequestPostProcessor from(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    private ResultActions login(String username, String ip) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).with(from(ip))
                .content(json.writeValueAsString(Map.of("username", username, "password", "x"))));
    }

    private ResultActions postJson(String path, Map<String, String> body, String ip) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).with(from(ip))
                .content(json.writeValueAsString(body)));
    }

    @Test
    void the11thLoginFromOneIpWithinAMinute_gets429() throws Exception {
        // mỗi lần dùng một username lạ để KHÔNG kích hoạt khóa theo (username, IP) — chỉ đo giới hạn theo IP
        for (int i = 1; i <= 10; i++) {
            login(unique("ghost"), "10.9.9.9").andExpect(status().isUnauthorized());
        }

        login(unique("ghost"), "10.9.9.9")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(errorMessageContains("quá nhanh"));
    }

    @Test
    void anotherIp_isNotAffected() throws Exception {
        for (int i = 1; i <= 11; i++) {
            login(unique("ghost"), "10.9.9.10");
        }

        login(unique("ghost"), "10.9.9.11").andExpect(status().isUnauthorized());   // 401 (sai mật khẩu), không phải 429
    }

    @Test
    void otpVerify_isRateLimitedPerIp() throws Exception {
        Map<String, String> body = Map.of("preAuthToken", "khong-phai-jwt", "code", "123456");
        for (int i = 1; i <= 3; i++) {
            postJson("/api/auth/otp/verify", body, "10.9.9.20").andExpect(status().isUnauthorized());
        }

        postJson("/api/auth/otp/verify", body, "10.9.9.20").andExpect(status().isTooManyRequests());
        postJson("/api/auth/otp/verify", body, "10.9.9.21").andExpect(status().isUnauthorized());
    }

    @Test
    void otpResend_isRateLimitedPerIp() throws Exception {
        Map<String, String> body = Map.of("preAuthToken", "khong-phai-jwt");
        postJson("/api/auth/otp/resend", body, "10.9.9.30").andExpect(status().isUnauthorized());
        postJson("/api/auth/otp/resend", body, "10.9.9.30").andExpect(status().isUnauthorized());

        postJson("/api/auth/otp/resend", body, "10.9.9.30").andExpect(status().isTooManyRequests());
    }
}
