package com.relivio.incident.service;

import com.relivio.incident.client.NotificationClient;
import com.relivio.incident.client.ReliefRequestClient;
import com.relivio.incident.dto.IncidentRequest;
import com.relivio.incident.dto.IncidentResponse;
import com.relivio.incident.dto.NotificationRequest;
import com.relivio.incident.dto.ReliefRequestSummary;
import com.relivio.incident.entity.Incident;
import com.relivio.incident.enums.IncidentStatus;
import com.relivio.incident.enums.NotificationPriority;
import com.relivio.incident.enums.NotificationType;
import com.relivio.incident.enums.RequestStatus;
import com.relivio.incident.enums.Severity;
import com.relivio.incident.exception.InvalidIncidentStateException;
import com.relivio.incident.exception.ResourceNotFoundException;
import com.relivio.incident.repository.IncidentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class IncidentServiceImpl implements IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentServiceImpl.class);

    private static final List<RequestStatus> OPEN_RELIEF_REQUEST_STATUSES =
            List.of(RequestStatus.PENDING, RequestStatus.ASSIGNED, RequestStatus.IN_PROGRESS);

    private static final Map<IncidentStatus, Set<IncidentStatus>> ALLOWED_TRANSITIONS = Map.of(
            IncidentStatus.REPORTED, EnumSet.of(IncidentStatus.VERIFIED, IncidentStatus.IN_PROGRESS, IncidentStatus.RESOLVED, IncidentStatus.CLOSED),
            IncidentStatus.VERIFIED, EnumSet.of(IncidentStatus.IN_PROGRESS, IncidentStatus.RESOLVED, IncidentStatus.CLOSED),
            IncidentStatus.IN_PROGRESS, EnumSet.of(IncidentStatus.RESOLVED, IncidentStatus.CLOSED),
            IncidentStatus.RESOLVED, EnumSet.of(IncidentStatus.CLOSED, IncidentStatus.REOPENED),
            IncidentStatus.CLOSED, EnumSet.of(IncidentStatus.REOPENED),
            IncidentStatus.REOPENED, EnumSet.of(IncidentStatus.IN_PROGRESS, IncidentStatus.RESOLVED, IncidentStatus.CLOSED)
    );

    private final IncidentRepository incidentRepository;
    private final ReliefRequestClient reliefRequestClient;
    private final NotificationClient notificationClient;

    public IncidentServiceImpl(IncidentRepository incidentRepository,
                               ReliefRequestClient reliefRequestClient,
                               NotificationClient notificationClient) {
        this.incidentRepository = incidentRepository;
        this.reliefRequestClient = reliefRequestClient;
        this.notificationClient = notificationClient;
    }

    @Override
    @Transactional
    public IncidentResponse createIncident(IncidentRequest incidentRequest) {
        log.info("Creating a new incident: {}", incidentRequest.getTitle());
        Incident incident = new Incident();
        BeanUtils.copyProperties(incidentRequest, incident);
        if (incident.getStatus() == null) {
            incident.setStatus(IncidentStatus.REPORTED);
        }
        incident = incidentRepository.save(incident);

        notifyAdmins("New incident reported",
                "A " + incident.getDisasterType() + " incident was reported at " + incident.getLocation()
                        + " with severity " + incident.getSeverity() + ".",
                NotificationType.INCIDENT_CREATED, incident.getIncidentId(), incident.getSeverity());

        log.info("Successfully created incident with ID: {}", incident.getIncidentId());
        return toResponse(incident);
    }

    @Override
    @Transactional(readOnly = true)
    public IncidentResponse getIncidentById(Long incidentId) {
        log.info("Fetching incident with ID: {}", incidentId);
        Incident incident = findIncidentById(incidentId);
        log.info("Successfully fetched incident with ID: {}", incidentId);
        return toResponse(incident);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncidentResponse> getAllIncidents() {
        log.info("Fetching all incidents");
        List<IncidentResponse> responses = incidentRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        log.info("Successfully fetched {} incidents", responses.size());
        return responses;
    }

    @Override
    @Transactional
    public IncidentResponse updateIncident(Long incidentId, IncidentRequest incidentRequest) {
        log.info("Updating incident with ID: {}. New data: {}", incidentId, incidentRequest.getTitle());
        Incident existingIncident = findIncidentById(incidentId);

        IncidentStatus newStatus = incidentRequest.getStatus() != null ? incidentRequest.getStatus() : existingIncident.getStatus();
        validateStatusTransition(existingIncident.getStatus(), newStatus, incidentId);

        BeanUtils.copyProperties(incidentRequest, existingIncident, "incidentId", "reportedDate", "status");
        existingIncident.setStatus(newStatus);
        existingIncident = incidentRepository.save(existingIncident);

        notifyStatusChange(existingIncident);
        log.info("Successfully updated incident with ID: {}", incidentId);
        return toResponse(existingIncident);
    }

    @Override
    @Transactional
    public void deleteIncident(Long incidentId) {
        log.info("Deleting incident with ID: {}", incidentId);
        if (!incidentRepository.existsById(incidentId)) {
            log.error("Incident not found with ID: {}", incidentId);
            throw new ResourceNotFoundException("Incident not found with id: " + incidentId);
        }
        incidentRepository.deleteById(incidentId);
        log.info("Successfully deleted incident with ID: {}", incidentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncidentResponse> getIncidentsByStatus(IncidentStatus status) {
        log.info("Fetching incidents with status: {}", status);
        return incidentRepository.findByStatus(status).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncidentResponse> getIncidentsBySeverity(Severity severity) {
        log.info("Fetching incidents with severity: {}", severity);
        return incidentRepository.findBySeverity(severity).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncidentResponse> getIncidentsByDisasterType(String disasterType) {
        log.info("Fetching incidents with disaster type: {}", disasterType);
        return incidentRepository.findByDisasterTypeIgnoreCase(disasterType).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncidentResponse> getIncidentsByLocation(String location) {
        log.info("Fetching incidents at/near location: {}", location);
        return incidentRepository.findByLocationContainingIgnoreCase(location).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public IncidentResponse patchIncident(Long incidentId, Map<String, Object> updates) {
        log.info("Patching incident with ID: {}. Updates: {}", incidentId, updates);
        Incident existingIncident = findIncidentById(incidentId);

        IncidentStatus previousStatus = existingIncident.getStatus();
        if (updates.containsKey("status")) {
            IncidentStatus newStatus = IncidentStatus.valueOf(String.valueOf(updates.get("status")));
            validateStatusTransition(previousStatus, newStatus, incidentId);
        }

        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            Field field = ReflectionUtils.findField(Incident.class, key);
            if (field == null) {
                log.warn("Unknown field '{}' ignored during patch of incident {}", key, incidentId);
                continue;
            }
            field.setAccessible(true);
            if (field.getType().isEnum()) {
                Object enumValue = Enum.valueOf((Class<Enum>) field.getType(), (String) value);
                ReflectionUtils.setField(field, existingIncident, enumValue);
            } else {
                ReflectionUtils.setField(field, existingIncident, value);
            }
        }

        existingIncident = incidentRepository.save(existingIncident);
        if (existingIncident.getStatus() != previousStatus) {
            notifyStatusChange(existingIncident);
        }
        log.info("Successfully patched incident with ID: {}", incidentId);
        return toResponse(existingIncident);
    }

    private void validateStatusTransition(IncidentStatus current, IncidentStatus target, Long incidentId) {
        if (current == target) {
            return;
        }
        Set<IncidentStatus> allowed = ALLOWED_TRANSITIONS.get(current);
        if (allowed == null || !allowed.contains(target)) {
            log.error("Invalid status transition for incident {}: {} -> {}", incidentId, current, target);
            throw new InvalidIncidentStateException("Invalid incident status transition: " + current + " -> " + target);
        }
        if (target == IncidentStatus.RESOLVED) {
            ensureNoOpenReliefRequests(incidentId);
        }
    }

    private void ensureNoOpenReliefRequests(Long incidentId) {
        try {
            List<ReliefRequestSummary> requests = reliefRequestClient.getReliefRequests(incidentId);
            boolean hasOpen = requests != null && requests.stream()
                    .anyMatch(r -> r.getStatus() != null && OPEN_RELIEF_REQUEST_STATUSES.contains(r.getStatus()));
            if (hasOpen) {
                log.error("Incident {} cannot be resolved while it has open relief requests", incidentId);
                throw new InvalidIncidentStateException(
                        "Incident cannot be marked Resolved while it still has relief requests in Pending or In Progress status.");
            }
        } catch (InvalidIncidentStateException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Could not verify relief requests for incident {} (service unavailable); proceeding gracefully. Cause: {}",
                    incidentId, ex.getMessage());
        }
    }

    private void notifyAdmins(String title, String message, NotificationType type, Long incidentId, Severity severity) {
        NotificationRequest notificationRequest = NotificationRequest.builder()
                .recipientId(1L)
                .recipientName("Administrator")
                .title(title)
                .message(message)
                .notificationType(type)
                .priority(severity == Severity.CRITICAL ? NotificationPriority.URGENT
                        : severity == Severity.HIGH ? NotificationPriority.HIGH
                        : severity == Severity.MEDIUM ? NotificationPriority.MEDIUM : NotificationPriority.LOW)
                .relatedEntityId(incidentId)
                .relatedEntityType("INCIDENT")
                .build();
        dispatchNotification(notificationRequest);
    }

    private void notifyStatusChange(Incident incident) {
        String message = "Incident '" + incident.getTitle() + "' status changed to " + incident.getStatus() + ".";
        NotificationRequest notificationRequest = NotificationRequest.builder()
                .recipientId(1L)
                .recipientName("Administrator")
                .title("Incident status updated")
                .message(message)
                .notificationType(NotificationType.INCIDENT_STATUS_CHANGED)
                .priority(NotificationPriority.HIGH)
                .relatedEntityId(incident.getIncidentId())
                .relatedEntityType("INCIDENT")
                .build();
        dispatchNotification(notificationRequest);
    }

    private void dispatchNotification(NotificationRequest request) {
        try {
            notificationClient.createNotification(request);
        } catch (Exception ex) {
            log.warn("Failed to create notification (non-blocking): {}", ex.getMessage());
        }
    }

    private Incident findIncidentById(Long incidentId) {
        return incidentRepository.findById(incidentId)
                .orElseThrow(() -> {
                    log.error("Incident not found with ID: {}", incidentId);
                    return new ResourceNotFoundException("Incident not found with id: " + incidentId);
                });
    }

    private IncidentResponse toResponse(Incident incident) {
        IncidentResponse response = new IncidentResponse();
        BeanUtils.copyProperties(incident, response);
        return response;
    }
}
