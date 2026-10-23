package com.doanltmmt.Backend.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** P3-08 — requestId có trong MDC suốt request (mọi dòng log), được dọn sau đó và theo sang luồng gửi email. */
class RequestIdPropagationTest {

    @AfterEach
    void clear() {
        MDC.clear();
    }

    @Test
    void filterPutsRequestIdInMdcDuringTheRequest_andClearsItAfter() throws Exception {
        AtomicReference<String> seen = new AtomicReference<>();
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/x");
        req.addHeader(RequestIdFilter.HEADER, "req-1");
        MockHttpServletResponse res = new MockHttpServletResponse();
        new RequestIdFilter().doFilter(req, res, new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response) {
                seen.set(MDC.get(RequestIdFilter.MDC_KEY));
            }
        });
        assertEquals("req-1", seen.get());
        assertEquals("req-1", res.getHeader(RequestIdFilter.HEADER));
        assertNull(MDC.get(RequestIdFilter.MDC_KEY));
    }

    @Test
    void mailExecutorCarriesTheRequestId() throws Exception {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) new AsyncConfig().mailExecutor(100);
        try {
            MDC.put(RequestIdFilter.MDC_KEY, "req-2");
            CompletableFuture<String> onMailThread = new CompletableFuture<>();
            executor.execute(() -> onMailThread.complete(MDC.get(RequestIdFilter.MDC_KEY)));
            assertEquals("req-2", onMailThread.get(5, TimeUnit.SECONDS));
            MDC.clear();
            CompletableFuture<String> later = new CompletableFuture<>();
            executor.execute(() -> later.complete(String.valueOf(MDC.get(RequestIdFilter.MDC_KEY))));
            assertEquals("null", later.get(5, TimeUnit.SECONDS)); // không rò sang tác vụ sau
        } finally {
            executor.shutdown();
        }
    }
}
