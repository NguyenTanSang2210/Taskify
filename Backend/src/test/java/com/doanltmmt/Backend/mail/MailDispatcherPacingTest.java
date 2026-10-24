package com.doanltmmt.Backend.mail;

import com.doanltmmt.Backend.service.EmailService;
import com.doanltmmt.Backend.service.MailDispatcher;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** P4-03 — gửi hàng loạt (email kích hoạt khi import) được giãn nhịp, kể cả khi nhiều luồng gửi cùng lúc. */
class MailDispatcherPacingTest {

    @Test
    @SuppressWarnings("unchecked")
    void consecutiveEmails_areSpacedByTheMinimumInterval_acrossThreads() throws Exception {
        JavaMailSender sender = mock(JavaMailSender.class);
        when(sender.createMimeMessage()).thenAnswer(i -> new MimeMessage(Session.getInstance(new Properties())));
        List<Long> sentAt = new ArrayList<>();
        doAnswer(i -> {
            synchronized (sentAt) {
                sentAt.add(System.nanoTime());
            }
            return null;
        }).when(sender).send(any(MimeMessage.class));
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        MailDispatcher dispatcher = new MailDispatcher(provider, "noreply@test.local", 10, 100);

        List<Thread> threads = new ArrayList<>();
        for (int t = 0; t < 2; t++) {
            Thread th = new Thread(() -> {
                for (int i = 0; i < 2; i++) {
                    dispatcher.onMailRequested(new EmailService.MailRequested("sv@test.local", "Kích hoạt", "<p>x</p>"));
                }
            });
            threads.add(th);
            th.start();
        }
        for (Thread th : threads) th.join();

        List<Long> sorted = sentAt.stream().sorted().toList();
        assertTrue(sorted.size() == 4, "gửi đủ 4 email");
        // Mỗi email được cấp một "suất" cách suất trước ≥ 100 ms; thời điểm gửi quan sát được còn lệch theo lập lịch
        // luồng (máy bận khi chạy cả bộ test) nên kiểm tra với dung sai: từng khoảng ≥ 50 ms, cả 4 email trải ≥ 250 ms.
        for (int i = 1; i < sorted.size(); i++) {
            long gapMillis = (sorted.get(i) - sorted.get(i - 1)) / 1_000_000;
            assertTrue(gapMillis >= 50, "hai email liên tiếp cách nhau " + gapMillis + " ms");
        }
        long spanMillis = (sorted.get(3) - sorted.get(0)) / 1_000_000;
        assertTrue(spanMillis >= 250, "4 email phải trải trên ≥ 3 khoảng 100 ms, thực tế " + spanMillis + " ms");
    }
}
