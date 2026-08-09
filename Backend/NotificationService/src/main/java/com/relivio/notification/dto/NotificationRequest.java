package com.relivio.notification.dto;

import com.relivio.notification.enums.NotificationPriority;
import com.relivio.notification.enums.NotificationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationRequest {

    @NotNull(message = "Recipient ID is mandatory")
    private Long recipientId;

    @NotBlank(message = "Recipient name is mandatory")
    @Size(max = 100, message = "Recipient name cannot exceed 100 characters")
    private String recipientName;

    @Email(message = "Recipient email should be in a valid format")
    @Size(max = 100, message = "Recipient email cannot exceed 100 characters")
    private String recipientEmail;

    @NotBlank(message = "Title is mandatory")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    @NotBlank(message = "Message is mandatory")
    @Size(max = 1000, message = "Message cannot exceed 1000 characters")
    private String message;

    @NotNull(message = "Notification type is mandatory")
    private NotificationType notificationType;

    @NotNull(message = "Priority is mandatory")
    private NotificationPriority priority;

    private Long relatedEntityId;

    @Size(max = 50, message = "Related entity type cannot exceed 50 characters")
    private String relatedEntityType;
}
