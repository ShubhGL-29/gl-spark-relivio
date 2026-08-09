package com.relivio.reliefrequest.dto;

import lombok.Data;

@Data
public class NotificationResponse {
    private Long notificationId;
    private Long recipientId;
    private String title;
    private String message;
}
