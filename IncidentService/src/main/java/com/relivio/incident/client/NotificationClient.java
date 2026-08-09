package com.relivio.incident.client;

import com.relivio.incident.dto.NotificationRequest;
import com.relivio.incident.dto.NotificationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service")
public interface NotificationClient {

    @PostMapping("/api/notifications")
    NotificationResponse createNotification(@RequestBody NotificationRequest request);
}
