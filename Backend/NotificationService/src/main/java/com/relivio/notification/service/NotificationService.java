package com.relivio.notification.service;

import com.relivio.notification.dto.NotificationRequest;
import com.relivio.notification.dto.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {

    NotificationResponse createNotification(NotificationRequest request);

    NotificationResponse getNotificationById(Long notificationId);

    List<NotificationResponse> getNotificationsByRecipient(Long recipientId);

    Page<NotificationResponse> getAllNotifications(Pageable pageable);

    Page<NotificationResponse> searchNotifications(String keyword, Pageable pageable);

    NotificationResponse markAsRead(Long notificationId);

    void deleteNotification(Long notificationId);

    long getUnreadCount(Long recipientId);
}
