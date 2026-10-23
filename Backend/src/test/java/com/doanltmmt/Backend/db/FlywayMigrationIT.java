package com.doanltmmt.Backend.db;

import org.flywaydb.core.Flyway;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * P3-01 — migration Flyway chạy đúng trên CSDL riêng {@value #SCHEMA} (tạo mới mỗi test, không đụng DB của ứng dụng):
 * DB trống → đủ schema + vai trò; DB cũ (tạo bằng ddl-auto, thiếu cột/bảng, dữ liệu kiểu cũ) → nâng cấp tại chỗ.
 * Cần quyền CREATE DATABASE: dùng FLYWAY_IT_USERNAME / FLYWAY_IT_PASSWORD nếu có, không thì tài khoản DB_USERNAME.
 */
class FlywayMigrationIT {

    private static final String SCHEMA = "taskify_flyway_it";

    private String serverUrl;
    private String user;
    private String password;

    @BeforeEach
    void recreateSchema() throws SQLException {
        String dbUrl = System.getenv("DB_URL");
        assumeTrue(dbUrl != null && dbUrl.startsWith("jdbc:mysql://"), "Cần DB_URL trỏ tới MySQL");
        serverUrl = dbUrl.replaceFirst("(jdbc:mysql://[^/]+/)[^?]*", "$1" + SCHEMA);
        user = env("FLYWAY_IT_USERNAME", env("DB_USERNAME", "root"));
        password = env("FLYWAY_IT_PASSWORD", env("DB_PASSWORD", ""));
        try (Connection c = DriverManager.getConnection(dbUrl.replaceFirst("(jdbc:mysql://[^/]+/)[^?]*", "$1"), user, password);
             Statement st = c.createStatement()) {
            st.execute("DROP DATABASE IF EXISTS " + SCHEMA);
            st.execute("CREATE DATABASE " + SCHEMA);
        } catch (SQLException e) {
            assumeTrue(false, "Tài khoản " + user + " không có quyền tạo CSDL thử nghiệm: " + e.getMessage());
        }
    }

    private static String env(String key, String fallback) {
        String v = System.getenv(key);
        return v == null || v.isBlank() ? fallback : v;
    }

    private Flyway flyway(String target) {
        var config = Flyway.configure().dataSource(serverUrl, user, password).locations("classpath:db/migration")
                .baselineOnMigrate(true).baselineVersion("0");
        if (target != null) config.target(target);
        return config.load();
    }

    private List<String> query(String sql) throws SQLException {
        try (Connection c = DriverManager.getConnection(serverUrl, user, password);
             Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            List<String> rows = new ArrayList<>();
            while (rs.next()) rows.add(rs.getString(1));
            return rows;
        }
    }

    private void exec(String... sqls) throws SQLException {
        try (Connection c = DriverManager.getConnection(serverUrl, user, password); Statement st = c.createStatement()) {
            for (String sql : sqls) st.execute(sql);
        }
    }

    private List<String> privilegesOf(String role) throws SQLException {
        return query("select p.name from roles r join roles_privileges rp on rp.role_id = r.id "
                + "join privileges p on p.id = rp.privilege_id where r.name = '" + role + "' order by p.name");
    }

    private boolean columnExists(String table, String column) throws SQLException {
        return !query("select 1 from information_schema.columns where table_schema = database() and table_name = '"
                + table + "' and column_name = '" + column + "'").isEmpty();
    }

    private boolean tableExists(String table) throws SQLException {
        return !query("select 1 from information_schema.tables where table_schema = database() and table_name = '" + table + "'").isEmpty();
    }

    @Test
    void emptyDatabase_getsFullSchema_andSystemRoles() throws SQLException {
        flyway(null).migrate();

        assertEquals("28", query("select count(*) from information_schema.tables where table_schema = database() "
                + "and table_name <> 'flyway_schema_history'").get(0));
        assertEquals(List.of("ADMIN", "DEPARTMENT_ADMIN", "LECTURER", "STUDENT"), query("select name from roles order by name"));
        assertEquals(List.of("REGISTRATION_MANAGE", "TOPIC_MANAGE"), privilegesOf("LECTURER"));
        assertEquals(6, privilegesOf("ADMIN").size());
        assertTrue(query("select version from flyway_schema_history where success = 1 order by installed_rank")
                .containsAll(List.of("1", "2", "3", "4", "5")));
        // V5 (P3-07): index cho truy vấn hay dùng
        assertEquals(List.of("idx_notifications_user_created", "idx_notifications_user_read"),
                query("select distinct index_name from information_schema.statistics where table_schema = database() "
                        + "and table_name = 'notifications' and index_name like 'idx_%' order by index_name"));
        flyway(null).validate();
    }

    @Test
    void legacyDatabase_withoutFlyway_isUpgradedInPlace() throws SQLException {
        // Dựng DB "cũ": schema V1 rồi làm thiếu đi như DB tạo từ mã trước GĐ2, có dữ liệu kiểu cũ, chưa có Flyway
        flyway("1").migrate();
        exec("DROP TABLE flyway_schema_history",
                "SET FOREIGN_KEY_CHECKS = 0",
                "DROP TABLE grade_history", "DROP TABLE grade_revision_requests",
                "ALTER TABLE progress_report DROP COLUMN similarity_score, DROP COLUMN similar_to_report_id",
                "ALTER TABLE users DROP COLUMN activated, DROP COLUMN token_version",
                "ALTER TABLE topics DROP FOREIGN KEY FK20bsyno5jeliiync88nsj7v0d",
                "ALTER TABLE otp_tokens MODIFY COLUMN purpose enum('ACTIVATION','LOGIN') NOT NULL DEFAULT 'LOGIN'",
                "CREATE TABLE project_permissions (id bigint PRIMARY KEY)",
                "INSERT INTO privileges (name) VALUES ('TOPIC_MANAGE'), ('REGISTRATION_MANAGE')",
                "INSERT INTO roles (name) VALUES ('ADMIN'), ('LECTURER')",
                "INSERT INTO roles_privileges SELECT r.id, p.id FROM roles r, privileges p "
                        + "WHERE r.name = 'LECTURER' AND p.name = 'REGISTRATION_MANAGE'",
                "INSERT INTO topics (id, title, status, capacity) VALUES (1, 'Đề tài cũ', 'REGISTERED', 2)",
                "INSERT INTO topic_registration (topic_id, student_id, approved, registered_at) VALUES (1, 1, 1, NOW())");

        flyway(null).migrate();

        assertTrue(tableExists("grade_history") && tableExists("grade_revision_requests"), "bảng mới được tạo");
        assertTrue(columnExists("progress_report", "similarity_score") && columnExists("progress_report", "similar_to_report_id"));
        // cột có giá trị mặc định (DB thời GĐ0 chưa có): tài khoản cũ được kích hoạt sẵn, token_version = 0
        assertEquals(List.of("b'1'"), query("select column_default from information_schema.columns where table_schema = database() "
                + "and table_name = 'users' and column_name = 'activated'"));
        assertTrue(columnExists("users", "token_version"));
        assertFalse(query("select 1 from information_schema.table_constraints where table_schema = database() "
                + "and constraint_name = 'FK20bsyno5jeliiync88nsj7v0d'").isEmpty(), "khóa ngoại được bổ sung");
        assertTrue(query("select column_type from information_schema.columns where table_schema = database() "
                + "and table_name = 'otp_tokens' and column_name = 'purpose'").get(0).contains("RESET_PASSWORD"));
        assertFalse(tableExists("project_permissions"), "bảng ProjectPermission rỗng bị xóa");
        assertEquals("OPEN", query("select status from topics where id = 1").get(0), "1/2 chỗ → còn mở");
        assertEquals(List.of("REGISTRATION_MANAGE", "TOPIC_MANAGE"), privilegesOf("LECTURER"), "P2-01 bổ sung TOPIC_MANAGE");
        assertTrue(privilegesOf("ADMIN").isEmpty(), "vai trò đã có: không gán lại quyền mặc định");
        assertEquals(List.of("ADMIN", "DEPARTMENT_ADMIN", "LECTURER", "STUDENT"), query("select name from roles order by name"));
        assertEquals("0", query("select version from flyway_schema_history order by installed_rank limit 1").get(0), "baseline");
        flyway(null).validate();
    }

    @Test
    void dataStepsAlreadyAppliedByTheOldRunner_areNotRepeated() throws SQLException {
        flyway("1").migrate();
        exec("DROP TABLE flyway_schema_history",
                "INSERT INTO privileges (name) VALUES ('TOPIC_MANAGE')",
                "INSERT INTO roles (name) VALUES ('LECTURER')",
                // Admin đã chủ động gỡ TOPIC_MANAGE khỏi giảng viên sau khi bước P2-01 chạy
                "INSERT INTO data_migrations (id, applied_at) VALUES ('P2-01_lecturer_topic_manage', NOW(6))");

        flyway(null).migrate();

        assertTrue(privilegesOf("LECTURER").isEmpty(), "không ghi đè quyền quản trị viên đã chỉnh");
    }

    @Test
    void legacyStatusValues_areNormalized_forEnums() throws SQLException {
        flyway("3").migrate();
        exec("SET FOREIGN_KEY_CHECKS = 0",
                "INSERT INTO workspaces (id, name, status, active, department_id) VALUES "
                        + "(1, 'cũ', 'open', 1, 1), (2, 'lạ', 'ACTIVE', 1, 1), (3, 'thường', ' in_progress ', 1, 1)",
                "INSERT INTO topics (id, title, status, capacity) VALUES (1, 'a', 'APPROVED', 1), (2, 'b', 'closed', 1)",
                "INSERT INTO notifications (user_id, type, title, created_at, severity) VALUES "
                        + "(1, 'info', 'x', NOW(), NULL), (1, 'ALERT', 'y', NOW(), 'high')");

        flyway(null).migrate();

        assertEquals(List.of("OPEN_TOPIC", "DRAFT", "IN_PROGRESS"), query("select status from workspaces order by id"));
        assertEquals(List.of("OPEN", "CLOSED"), query("select status from topics order by id"));
        assertEquals(List.of("INFO:INFO", "INFO:INFO"), query("select concat(type, ':', severity) from notifications order by id"));
    }
}
