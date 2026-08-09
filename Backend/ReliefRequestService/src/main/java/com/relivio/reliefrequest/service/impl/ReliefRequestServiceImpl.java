package com.relivio.reliefrequest.service.impl;

import com.relivio.reliefrequest.client.IncidentClient;
import com.relivio.reliefrequest.client.NotificationClient;
import com.relivio.reliefrequest.dto.IncidentResponse;
import com.relivio.reliefrequest.dto.NotificationRequest;
import com.relivio.reliefrequest.dto.ReliefRequestRequest;
import com.relivio.reliefrequest.dto.ReliefRequestResponse;
import com.relivio.reliefrequest.entity.ReliefRequest;
import com.relivio.reliefrequest.enums.NotificationPriority;
import com.relivio.reliefrequest.enums.NotificationType;
import com.relivio.reliefrequest.enums.Priority;
import com.relivio.reliefrequest.enums.RequestStatus;
import com.relivio.reliefrequest.exception.InvalidRequestStateException;
import com.relivio.reliefrequest.exception.ResourceNotFoundException;
import com.relivio.reliefrequest.repository.ReliefRequestRepository;
import com.relivio.reliefrequest.service.ReliefRequestService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReliefRequestServiceImpl implements ReliefRequestService {

    private static final Logger log = LoggerFactory.getLogger(ReliefRequestServiceImpl.class);

    private static final List<RequestStatus> OPEN_STATUSES =
            List.of(RequestStatus.PENDING, RequestStatus.ASSIGNED, RequestStatus.IN_PROGRESS);

    private static final Map<RequestStatus, Set<RequestStatus>> ALLOWED_TRANSITIONS = Map.of(
            RequestStatus.PENDING, EnumSet.of(RequestStatus.ASSIGNED, RequestStatus.IN_PROGRESS, RequestStatus.CANCELLED),
            RequestStatus.ASSIGNED, EnumSet.of(RequestStatus.IN_PROGRESS, RequestStatus.FULFILLED, RequestStatus.CANCELLED),
            RequestStatus.IN_PROGRESS, EnumSet.of(RequestStatus.FULFILLED, RequestStatus.CANCELLED),
            RequestStatus.FULFILLED, EnumSet.of(RequestStatus.CLOSED),
            RequestStatus.CLOSED, EnumSet.noneOf(RequestStatus.class),
            RequestStatus.CANCELLED, EnumSet.noneOf(RequestStatus.class)
    );

    private static final Set<String> SAFE_TO_PATCH_FIELDS = Set.of(
            "status", "priority", "description",
            "assignedVolunteerId", "assignedVolunteerName",
            "allocatedResourceId", "allocatedResourceName",
            "allocatedShelterId", "allocatedShelterName"
    );

    private final ReliefRequestRepository reliefRequestRepository;
    private final IncidentClient incidentClient;
    private final NotificationClient notificationClient;

    @Override
    @Transactional
    public ReliefRequestResponse createRequest(ReliefRequestRequest request) {
        log.info("Creating new relief request for incident ID: {}", request.getIncidentId());

        IncidentResponse incident;
        try {
            incident = incidentClient.getIncidentById(request.getIncidentId());
        } catch (Exception ex) {
            log.error("IncidentService is unavailable or incident {} does not exist. Cause: {}",
                    request.getIncidentId(), ex.getMessage());
            throw new ResourceNotFoundException("Incident not found with id: " + request.getIncidentId() +
                    ". A relief request must be linked to a valid incident.");
        }

        if (incident == null) {
            throw new ResourceNotFoundException("Incident not found with id: " + request.getIncidentId());
        }

        ReliefRequest reliefRequest = new ReliefRequest();
        BeanUtils.copyProperties(request, reliefRequest);
        reliefRequest.setAddress(StringUtils.hasText(incident.getLocation()) ? incident.getLocation() : "Unknown location");
        reliefRequest.setStatus(RequestStatus.PENDING);

        ReliefRequest savedRequest = reliefRequestRepository.save(reliefRequest);

        notifyRecipient(savedRequest,
                "Relief request submitted",
                "Your " + savedRequest.getRequestType() + " relief request (#" + savedRequest.getRequestId()
                        + ") has been submitted and is now pending. A responder will be assigned shortly.",
                NotificationType.RELIEF_REQUEST_CREATED,
                savedRequest.getPriority());

        log.info("Successfully created and saved relief request with ID: {}", savedRequest.getRequestId());
        return toResponse(savedRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public ReliefRequestResponse getRequestById(Long requestId) {
        log.info("Fetching relief request with ID: {}", requestId);
        return toResponse(findRequestById(requestId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReliefRequestResponse> getAllRequests() {
        log.info("Fetching all relief requests");
        return reliefRequestRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReliefRequestResponse> getRequestsByIncidentId(Long incidentId) {
        log.info("Fetching relief requests for incident ID: {}", incidentId);
        return reliefRequestRepository.findByIncidentId(incidentId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReliefRequestResponse> getRequestsByStatus(RequestStatus status) {
        log.info("Fetching relief requests with status: {}", status);
        return reliefRequestRepository.findByStatus(status).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReliefRequestResponse> getRequestsByPriority(Priority priority) {
        log.info("Fetching relief requests with priority: {}", priority);
        return reliefRequestRepository.findByPriority(priority).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReliefRequestResponse> getOpenRequests(Long incidentId) {
        log.info("Fetching open relief requests for incident ID: {}", incidentId);
        List<ReliefRequest> requests = incidentId != null
                ? reliefRequestRepository.findByIncidentIdAndStatusIn(incidentId, OPEN_STATUSES)
                : reliefRequestRepository.findByStatusIn(OPEN_STATUSES);
        return requests.stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReliefRequestResponse updateRequest(Long requestId, ReliefRequestRequest request) {
        log.info("Updating relief request with ID: {}", requestId);
        ReliefRequest existingRequest = findRequestById(requestId);

        if (existingRequest.getStatus() == RequestStatus.FULFILLED
                || existingRequest.getStatus() == RequestStatus.CLOSED
                || existingRequest.getStatus() == RequestStatus.CANCELLED) {
            log.error("Attempted to update a request that is already in a terminal state. ID: {}", requestId);
            throw new InvalidRequestStateException("Cannot update a request that is already fulfilled, closed, or cancelled.");
        }

        RequestStatus newStatus = request.getStatus() != null ? request.getStatus() : existingRequest.getStatus();
        validateStatusTransition(existingRequest, newStatus, requestId);

        BeanUtils.copyProperties(request, existingRequest, "requestId", "requestDate", "address", "status",
                "assignedVolunteerId", "allocatedResourceId", "allocatedShelterId");
        existingRequest.setStatus(newStatus);
        ReliefRequest updatedRequest = reliefRequestRepository.save(existingRequest);

        notifyStatusChange(updatedRequest);
        log.info("Successfully updated relief request with ID: {}", updatedRequest.getRequestId());
        return toResponse(updatedRequest);
    }

    @Override
    @Transactional
    public ReliefRequestResponse patchRequest(Long requestId, Map<String, Object> updates) {
        log.info("Patching relief request with ID: {}. Updates: {}", requestId, updates);
        ReliefRequest existingRequest = findRequestById(requestId);

        if (updates.containsKey("status")) {
            RequestStatus newStatus = RequestStatus.valueOf(String.valueOf(updates.get("status")));
            try {
                validateStatusTransition(existingRequest, newStatus, requestId);
            } catch (InvalidRequestStateException e) {
                boolean linkPatch = updates.containsKey("assignedVolunteerId") || updates.containsKey("allocatedResourceId")
                        || updates.containsKey("allocatedShelterId");
                if (linkPatch) {
                    log.warn("Skipping status change {} -> {} during link patch for relief request {}: {}",
                            existingRequest.getStatus(), newStatus, requestId, e.getMessage());
                    updates.remove("status");
                } else {
                    throw e;
                }
            }
        }

        updates.forEach((key, value) -> {
            if (!SAFE_TO_PATCH_FIELDS.contains(key)) {
                log.warn("Attempted to patch a forbidden field: {} for relief request ID: {}", key, requestId);
                throw new IllegalArgumentException("Field '" + key + "' cannot be updated via this endpoint.");
            }
            Field field = ReflectionUtils.findField(ReliefRequest.class, key);
            if (field == null) {
                log.warn("Unknown field '{}' ignored during patch of relief request {}", key, requestId);
                return;
            }
            field.setAccessible(true);
            if (value == null) {
                ReflectionUtils.setField(field, existingRequest, null);
            } else if (field.getType().isEnum()) {
                Object enumValue = Enum.valueOf((Class<Enum>) field.getType(), (String) value);
                ReflectionUtils.setField(field, existingRequest, enumValue);
            } else if (field.getType() == Long.class) {
                ReflectionUtils.setField(field, existingRequest, Long.valueOf(value.toString()));
            } else {
                ReflectionUtils.setField(field, existingRequest, value);
            }
        });

        ReliefRequest patchedRequest = reliefRequestRepository.save(existingRequest);
        notifyStatusChange(patchedRequest);
        log.info("Successfully patched relief request with ID: {}", patchedRequest.getRequestId());
        return toResponse(patchedRequest);
    }

    @Override
    @Transactional
    public void deleteRequest(Long requestId) {
        log.info("Deleting relief request with ID: {}", requestId);
        ReliefRequest request = findRequestById(requestId);

        if (request.getStatus() == RequestStatus.IN_PROGRESS || request.getStatus() == RequestStatus.FULFILLED) {
            log.error("Attempted to delete a request that is in progress or fulfilled. ID: {}", requestId);
            throw new InvalidRequestStateException("Cannot delete a request that is in progress or fulfilled.");
        }

        reliefRequestRepository.delete(request);
        log.info("Successfully deleted relief request with ID: {}", requestId);
    }

    private void validateStatusTransition(ReliefRequest request, RequestStatus target, Long requestId) {
        RequestStatus current = request.getStatus();
        if (current == target) {
            return;
        }
        Set<RequestStatus> allowed = ALLOWED_TRANSITIONS.get(current);
        if (allowed == null || !allowed.contains(target)) {
            log.error("Invalid relief request status transition: {} -> {}", current, target);
            throw new InvalidRequestStateException("Invalid relief request status transition: " + current + " -> " + target);
        }
        if (target == RequestStatus.FULFILLED) {
            if (request.getAssignedVolunteerId() == null && request.getAllocatedResourceId() == null
                    && request.getAllocatedShelterId() == null) {
                log.error("Relief request {} cannot be fulfilled without a volunteer assignment, resource allocation, or shelter allocation", requestId);
                throw new InvalidRequestStateException(
                        "A relief request cannot be marked Fulfilled unless it has an assigned volunteer, an allocated resource, or an allocated shelter.");
            }
        }
    }

    private void notifyRecipient(ReliefRequest request, String title, String message,
                                 NotificationType type, Priority priority) {
        NotificationRequest notificationRequest = NotificationRequest.builder()
                .recipientId(request.getIncidentId())
                .recipientName(request.getVictimName())
                .recipientEmail(request.getEmail())
                .title(title)
                .message(message)
                .notificationType(type)
                .priority(toNotificationPriority(priority))
                .relatedEntityId(request.getRequestId())
                .relatedEntityType("RELIEF_REQUEST")
                .build();
        dispatchNotification(notificationRequest);
    }

    private void notifyStatusChange(ReliefRequest request) {
        String message = "Your " + request.getRequestType() + " relief request (#" + request.getRequestId()
                + ") status is now " + request.getStatus() + ".";
        notifyRecipient(request, "Relief request status updated", message,
                NotificationType.RELIEF_REQUEST_STATUS_CHANGED, request.getPriority());
    }

    private NotificationPriority toNotificationPriority(Priority priority) {
        if (priority == null) {
            return NotificationPriority.MEDIUM;
        }
        return switch (priority) {
            case URGENT -> NotificationPriority.URGENT;
            case HIGH -> NotificationPriority.HIGH;
            case LOW -> NotificationPriority.LOW;
            default -> NotificationPriority.MEDIUM;
        };
    }

    private void dispatchNotification(NotificationRequest request) {
        try {
            notificationClient.createNotification(request);
        } catch (Exception ex) {
            log.warn("Failed to create notification (non-blocking): {}", ex.getMessage());
        }
    }

    private ReliefRequest findRequestById(Long requestId) {
        return reliefRequestRepository.findById(requestId)
                .orElseThrow(() -> {
                    log.error("Relief request not found with ID: {}", requestId);
                    return new ResourceNotFoundException("Relief request not found with ID: " + requestId);
                });
    }

    private ReliefRequestResponse toResponse(ReliefRequest reliefRequest) {
        ReliefRequestResponse response = new ReliefRequestResponse();
        BeanUtils.copyProperties(reliefRequest, response);
        return response;
    }
}
