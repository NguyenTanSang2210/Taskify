package com.doanltmmt.Backend.service;

import com.doanltmmt.Backend.entity.Notification;
import com.doanltmmt.Backend.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** P3-05 — sự kiện WebSocket chỉ được gửi sau khi transaction commit (không gửi khi rollback). */
class EventPublisherTest {

    private final SimpMessagingTemplate template = mock(SimpMessagingTemplate.class);
    private final EventPublisher publisher = new EventPublisher(template);

    private static Notification notification() {
        User u = new User();
        u.setId(1L);
        u.setUsername("sv1");
        Notification n = new Notification();
        n.setId(9L);
        n.setUser(u);
        n.setTitle("Tiêu đề");
        return n;
    }

    @AfterEach
    void clear() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void withoutTransaction_sendsImmediately() {
        publisher.notificationCreated(notification());
        verify(template).convertAndSendToUser(eq("sv1"), eq("/queue/notifications"), any(Object.class));
    }

    @Test
    void insideTransaction_sendsOnlyAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        publisher.notificationCreated(notification());
        verify(template, never()).convertAndSendToUser(any(), any(), any(Object.class));
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(template).convertAndSendToUser(eq("sv1"), eq("/queue/notifications"), any(Object.class));
    }

    @Test
    void rolledBack_neverSends() {
        TransactionSynchronizationManager.initSynchronization();
        publisher.notificationCreated(notification());
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verify(template, never()).convertAndSendToUser(any(), any(), any(Object.class));
    }
}
