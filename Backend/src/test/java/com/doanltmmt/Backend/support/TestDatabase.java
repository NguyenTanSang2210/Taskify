package com.doanltmmt.Backend.support;

import org.testcontainers.containers.MySQLContainer;

/**
 * P3-09 — CSDL cho test. Có biến môi trường {@code DB_URL} (MySQL cài sẵn / service của CI cũ) → dùng nó như trước;
 * không có → tự chạy MySQL 8.4 trong Docker (Testcontainers) một lần cho cả lượt test, nên {@code ./mvnw verify}
 * chạy được trên máy chỉ có Docker. Container được Testcontainers (Ryuk) dọn khi JVM kết thúc.
 */
public final class TestDatabase {

    /** {@code admin*}: tài khoản có quyền CREATE DATABASE (FlywayMigrationIT tạo schema riêng). */
    public record Info(String url, String username, String password, String adminUser, String adminPassword,
                       boolean containerized) {
    }

    private static Info info;

    private TestDatabase() {
    }

    public static synchronized Info get() {
        if (info == null) info = resolve();
        return info;
    }

    private static Info resolve() {
        String url = System.getenv("DB_URL");
        if (url != null && !url.isBlank()) {
            String user = env("DB_USERNAME", "root");
            String password = env("DB_PASSWORD", "");
            return new Info(url, user, password, env("FLYWAY_IT_USERNAME", user), env("FLYWAY_IT_PASSWORD", password), false);
        }
        MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
                .withDatabaseName("doan_ltmmt").withUsername("ktpm").withPassword("ktpm123");
        mysql.start();
        String jdbc = mysql.getJdbcUrl() + (mysql.getJdbcUrl().contains("?") ? "&" : "?")
                + "useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        // Testcontainers đặt MYSQL_ROOT_PASSWORD = mật khẩu của user ứng dụng
        return new Info(jdbc, mysql.getUsername(), mysql.getPassword(), "root", mysql.getPassword(), true);
    }

    private static String env(String key, String fallback) {
        String v = System.getenv(key);
        return v == null || v.isBlank() ? fallback : v;
    }
}
