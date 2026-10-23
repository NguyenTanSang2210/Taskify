package com.doanltmmt.Backend.support;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

/**
 * P3-09 — chạy trước khi Spring đọc cấu hình (đăng ký trong {@code META-INF/spring.factories} của test):
 * không chỉ định profile → dùng {@code dev}; CSDL lấy từ {@link TestDatabase} (DB_URL hoặc Testcontainers).
 */
public class TestDatabaseEnvironment implements EnvironmentPostProcessor, Ordered {

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.getActiveProfiles().length == 0 && !environment.containsProperty("spring.profiles.active")) {
            environment.setDefaultProfiles("dev");
        }
        TestDatabase.Info db = TestDatabase.get();
        if (db.containerized()) {
            environment.getPropertySources().addFirst(new MapPropertySource("testcontainers-mysql", Map.of(
                    "spring.datasource.url", db.url(),
                    "spring.datasource.username", db.username(),
                    "spring.datasource.password", db.password())));
        }
    }
}
