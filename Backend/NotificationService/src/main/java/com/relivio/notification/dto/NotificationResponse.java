package com.relivio.notification.dto;

import com.relivio.notification.enums.NotificationPriority;
import com.relivio.notification.enums.NotificationStatus;
import com.relivio.notification.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private Long notificationId;
    private Long recipientId;
    private String recipientName;
    private String recipientEmail;
    private String title;
    private String message;
    private NotificationType notificationType;
    private NotificationPriority priority;
    private NotificationStatus status;
    private Long relatedEntityId;
    private String relatedEntityType;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
