package com.doanltmmt.Backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@Configuration
public class JacksonConfig {

    /**
     * Ngày giờ trong request nhận cả {@code yyyy-MM-ddTHH:mm[:ss]} lẫn {@code yyyy-MM-dd} (đầu ngày — ô chọn ngày của
     * form gửi dạng này); chuỗi rỗng → {@code null}. Sai định dạng → lỗi 400 có tên trường (GlobalExceptionHandler).
     * Đăng ký dạng bean Module: Spring Boot cài sau JavaTimeModule nên bộ đọc này được ưu tiên.
     */
    @Bean
    public Module lenientLocalDateTime() {
        SimpleModule module = new SimpleModule("taskify-lenient-datetime");
        module.addDeserializer(LocalDateTime.class, new JsonDeserializer<>() {
            @Override
            public LocalDateTime deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
                String text = p.getValueAsString();
                if (text == null || text.isBlank()) return null;
                String value = text.trim();
                try {
                    return value.length() == 10 ? LocalDate.parse(value).atStartOfDay() : LocalDateTime.parse(value);
                } catch (DateTimeParseException e) {
                    return (LocalDateTime) ctx.handleWeirdStringValue(LocalDateTime.class, value, "yyyy-MM-ddTHH:mm");
                }
            }
        });
        return module;
    }
}
