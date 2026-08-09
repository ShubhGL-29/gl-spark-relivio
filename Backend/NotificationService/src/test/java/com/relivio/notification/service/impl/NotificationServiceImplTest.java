package com.relivio.notification.service.impl;

import com.relivio.notification.dto.NotificationRequest;
import com.relivio.notification.dto.NotificationResponse;
import com.relivio.notification.entity.Notification;
import com.relivio.notification.enums.NotificationPriority;
import com.relivio.notification.enums.NotificationStatus;
import com.relivio.notification.enums.NotificationType;
import com.relivio.notification.exception.NotificationNotFoundException;
import com.relivio.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Notification notification;
    private NotificationRequest notificationRequest;

    @BeforeEach
    void setUp() {
        notification = Notification.builder()
                .notificationId(1L)
                .recipientId(100L)
                .recipientName("Jane Doe")
                .title("New Incident Reported")
                .message("A flood incident has been reported.")
                .notificationType(NotificationType.INCIDENT_CREATED)
                .priority(NotificationPriority.HIGH)
                .status(NotificationStatus.UNREAD)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRequest = NotificationRequest.builder()
                .recipientId(100L)
                .recipientName("Jane Doe")
                .title("New Incident Reported")
                .message("A flood incident has been reported.")
                .notificationType(NotificationType.INCIDENT_CREATED)
                .priority(NotificationPriority.HIGH)
                .build();
    }

    @Test
    void createNotification_shouldSaveWithUnreadStatus() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        NotificationResponse response = notificationService.createNotification(notificationRequest);

        assertNotNull(response);
        assertEquals(NotificationStatus.UNREAD, response.getStatus());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void getNotificationById_whenExists_shouldReturnResponse() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        NotificationResponse response = notificationService.getNotificationById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getNotificationId());
        assertEquals("Jane Doe", response.getRecipientName());
    }

    @Test
    void getNotificationById_whenNotExists_shouldThrow() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(NotificationNotFoundException.class, () -> notificationService.getNotificationById(1L));
    }

    @Test
    void getNotificationsByRecipient_shouldReturnList() {
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(100L)).thenReturn(List.of(notification));

        List<NotificationResponse> responses = notificationService.getNotificationsByRecipient(100L);

        assertNotNull(responses);
        assertEquals(1, responses.size());
    }

    @Test
    void getNotificationsByRecipient_whenNone_shouldReturnEmptyList() {
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(999L)).thenReturn(List.of());

        List<NotificationResponse> responses = notificationService.getNotificationsByRecipient(999L);

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    void markAsRead_shouldSetReadStatusAndReadAt() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        NotificationResponse response = notificationService.markAsRead(1L);

        assertEquals(NotificationStatus.READ, response.getStatus());
        assertNotNull(response.getReadAt());
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void deleteNotification_whenExists_shouldDelete() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));
        doNothing().when(notificationRepository).delete(notification);

        notificationService.deleteNotification(1L);

        verify(notificationRepository, times(1)).delete(notification);
    }

    @Test
    void getUnreadCount_shouldReturnCount() {
        when(notificationRepository.countByRecipientIdAndStatus(100L, NotificationStatus.UNREAD)).thenReturn(3L);

        long count = notificationService.getUnreadCount(100L);

        assertEquals(3L, count);
    }
}
