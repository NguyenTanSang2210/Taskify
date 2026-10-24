package com.doanltmmt.Backend.config;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/** Gửi email trên luồng riêng: request (đăng nhập OTP, duyệt đăng ký...) không phải chờ máy chủ SMTP. */
@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String MAIL_EXECUTOR = "mailExecutor";

    @Bean(name = MAIL_EXECUTOR)
    public Executor mailExecutor(@Value("${app.mail.queue-capacity:5000}") int queueCapacity) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        // Hàng đợi đủ lớn cho một lần import hàng loạt (P4-03: mỗi tài khoản một email kích hoạt). Đầy thì luồng gọi
        // tự gửi (CallerRuns) — chậm lại chứ không bỏ email như AbortPolicy mặc định.
        executor.setQueueCapacity(queueCapacity);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadNamePrefix("mail-");
        // Mang mã yêu cầu (MDC) sang luồng gửi mail → log gửi mail nối được với request gây ra nó
        executor.setTaskDecorator(task -> {
            Map<String, String> context = MDC.getCopyOfContextMap();
            return () -> {
                if (context != null) MDC.setContextMap(context);
                try {
                    task.run();
                } finally {
                    MDC.clear();
                }
            };
        });
        // Tắt ứng dụng: gửi nốt email đang chờ (tối đa 10 giây)
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }
}
