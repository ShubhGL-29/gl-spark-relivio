package com.relivio.notification.repository;

import com.relivio.notification.entity.Notification;
import com.relivio.notification.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    long countByRecipientIdAndStatus(Long recipientId, NotificationStatus status);

    List<Notification> findByTitleContainingIgnoreCaseOrMessageContainingIgnoreCase(String title, String message);
}
