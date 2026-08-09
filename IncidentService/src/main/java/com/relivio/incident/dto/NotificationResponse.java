package com.relivio.incident.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationResponse {
    private Long notificationId;
    private Long recipientId;
    private String title;
    private String message;
    private LocalDateTime createdAt;
}
