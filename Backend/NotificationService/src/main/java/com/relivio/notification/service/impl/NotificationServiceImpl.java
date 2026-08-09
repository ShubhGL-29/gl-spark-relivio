package com.relivio.notification.service.impl;

import com.relivio.notification.dto.NotificationRequest;
import com.relivio.notification.dto.NotificationResponse;
import com.relivio.notification.entity.Notification;
import com.relivio.notification.enums.NotificationStatus;
import com.relivio.notification.exception.NotificationNotFoundException;
import com.relivio.notification.repository.NotificationRepository;
import com.relivio.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public NotificationResponse createNotification(NotificationRequest request) {
        log.info("Creating notification for recipient {}: {}", request.getRecipientId(), request.getTitle());
        Notification notification = new Notification();
        BeanUtils.copyProperties(request, notification);
        notification.setStatus(NotificationStatus.UNREAD);
        Notification saved = notificationRepository.save(notification);
        log.info("Successfully created notification with ID: {}", saved.getNotificationId());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getNotificationById(Long notificationId) {
        return toResponse(findNotificationById(notificationId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByRecipient(Long recipientId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getAllNotifications(Pageable pageable) {
        return notificationRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> searchNotifications(String keyword, Pageable pageable) {
        List<Notification> matches = notificationRepository
                .findByTitleContainingIgnoreCaseOrMessageContainingIgnoreCase(keyword, keyword);
        return new org.springframework.data.domain.PageImpl<>(
                matches.stream().map(this::toResponse).collect(Collectors.toList()), pageable, matches.size());
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long notificationId) {
        Notification notification = findNotificationById(notificationId);
        if (notification.getStatus() == NotificationStatus.READ) {
            return toResponse(notification);
        }
        notification.setStatus(NotificationStatus.READ);
        notification.setReadAt(LocalDateTime.now());
        Notification updated = notificationRepository.save(notification);
        log.info("Marked notification {} as read", notificationId);
        return toResponse(updated);
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId) {
        Notification notification = findNotificationById(notificationId);
        notificationRepository.delete(notification);
        log.info("Deleted notification with ID: {}", notificationId);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long recipientId) {
        return notificationRepository.countByRecipientIdAndStatus(recipientId, NotificationStatus.UNREAD);
    }

    private Notification findNotificationById(Long notificationId) {
        return notificationRepository.findById(notificationId)
                .orElseThrow(() -> {
                    log.warn("Notification not found with ID: {}", notificationId);
                    return new NotificationNotFoundException("Notification not found with id: " + notificationId);
                });
    }

    private NotificationResponse toResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        BeanUtils.copyProperties(notification, response);
        return response;
    }
}
