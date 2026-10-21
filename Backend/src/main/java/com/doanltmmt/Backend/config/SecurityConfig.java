package com.doanltmmt.Backend.config;

import com.doanltmmt.Backend.security.JwtAuthenticationFilter;
import com.doanltmmt.Backend.security.SecurityErrorHandlers;
import com.doanltmmt.Backend.service.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Các endpoint công khai (không cần đăng nhập) — liệt kê TỪNG cái (mặc định từ chối): thêm endpoint mới
     * dưới /api/auth sẽ KHÔNG vô tình trở nên công khai.
     */
    private static final String[] SWAGGER_ENDPOINTS = {"/swagger-ui/**", "/swagger-ui.html", "/api-docs/**"};

    private static final String[] PUBLIC_AUTH_ENDPOINTS = {
            "/api/auth/login",
            "/api/auth/otp/verify",
            "/api/auth/otp/resend",
            "/api/auth/activate",
            "/api/auth/password/initial",
            "/api/auth/password/forgot",
            "/api/auth/password/reset"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          CustomUserDetailsService userDetailsService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   @Value("${app.swagger.public:false}") boolean swaggerPublic) throws Exception {
        http
                // CSRF không áp dụng: xác thực bằng header Authorization (Bearer), không dùng cookie/session.
                // Khi có refresh token trong cookie (P3-14) phải bảo vệ CSRF cho endpoint đó (SameSite + kiểm tra Origin).
                .csrf(csrf -> csrf.disable())
                // Bật CORS trong Spring Security để preflight/POST từ frontend không bị 403
                .cors(cors -> {
                })
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 401 cho chưa đăng nhập/token hỏng/hết hạn/bị thu hồi; 403 cho đã đăng nhập nhưng không đủ quyền
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(SecurityErrorHandlers.unauthorized())
                        .accessDeniedHandler(SecurityErrorHandlers.forbidden()))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/error").permitAll()
                            .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                            .requestMatchers(HttpMethod.POST, PUBLIC_AUTH_ENDPOINTS).permitAll()
                            .requestMatchers("/ws/**").permitAll();
                    // Swagger chỉ mở ở môi trường dev (app.swagger.public=true); production tắt hẳn (springdoc.*.enabled=false)
                    if (swaggerPublic) {
                        auth.requestMatchers(SWAGGER_ENDPOINTS).permitAll();
                    }
                    auth.anyRequest().authenticated();
                })

                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }
}
