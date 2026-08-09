package com.relivio.volunteer.service;

import com.relivio.volunteer.client.NotificationClient;
import com.relivio.volunteer.client.ReliefRequestClient;
import com.relivio.volunteer.dto.AssignmentRequest;
import com.relivio.volunteer.dto.NotificationRequest;
import com.relivio.volunteer.dto.VolunteerRequest;
import com.relivio.volunteer.dto.VolunteerResponse;
import com.relivio.volunteer.dto.VolunteerStatusUpdateRequest;
import com.relivio.volunteer.entity.Volunteer;
import com.relivio.volunteer.enums.AvailabilityStatus;
import com.relivio.volunteer.enums.NotificationPriority;
import com.relivio.volunteer.enums.NotificationType;
import com.relivio.volunteer.exception.DuplicateResourceException;
import com.relivio.volunteer.exception.InvalidVolunteerStateException;
import com.relivio.volunteer.exception.ResourceNotFoundException;
import com.relivio.volunteer.repository.VolunteerRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VolunteerServiceImpl implements VolunteerService {

    private static final Logger log = LoggerFactory.getLogger(VolunteerServiceImpl.class);

    private final VolunteerRepository volunteerRepository;
    private final NotificationClient notificationClient;
    private final ReliefRequestClient reliefRequestClient;

    private static final Set<String> SAFE_TO_PATCH_FIELDS = Set.of(
            "address", "city", "state", "phoneNumber", "emergencyContactName",
            "emergencyContactNumber", "skills"
    );

    @Override
    @Transactional
    public VolunteerResponse registerVolunteer(VolunteerRequest volunteerRequest) {
        log.info("Registering new volunteer with email: {}", volunteerRequest.getEmail());
        validateUniqueFields(volunteerRequest.getEmail(), volunteerRequest.getPhoneNumber(), null);

        Volunteer volunteer = new Volunteer();
        BeanUtils.copyProperties(volunteerRequest, volunteer);

        if (volunteerRequest.getAvailabilityStatus() != null) {
            volunteer.setAvailabilityStatus(volunteerRequest.getAvailabilityStatus());
        } else {
            volunteer.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        }

        Volunteer savedVolunteer = volunteerRepository.save(volunteer);
        log.info("Successfully registered volunteer with ID: {}", savedVolunteer.getVolunteerId());

        dispatchNotification(NotificationRequest.builder()
                .recipientId(savedVolunteer.getVolunteerId())
                .recipientName(savedVolunteer.getFirstName() + " " + savedVolunteer.getLastName())
                .recipientEmail(savedVolunteer.getEmail())
                .title("Welcome to Relivio")
                .message("Your volunteer profile has been registered. Stay available to help when a disaster strikes.")
                .notificationType(NotificationType.GENERAL)
                .priority(NotificationPriority.LOW)
                .build());

        return toResponse(savedVolunteer);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VolunteerResponse> getAllVolunteers(Pageable pageable) {
        log.info("Fetching all volunteers for page: {} and size: {}", pageable.getPageNumber(), pageable.getPageSize());
        return volunteerRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public VolunteerResponse getVolunteerById(Long volunteerId) {
        log.info("Fetching volunteer with ID: {}", volunteerId);
        return toResponse(findVolunteerById(volunteerId));
    }

    @Override
    @Transactional
    public VolunteerResponse updateVolunteer(Long volunteerId, VolunteerRequest volunteerRequest) {
        log.info("Updating volunteer with ID: {}", volunteerId);
        Volunteer existingVolunteer = findVolunteerById(volunteerId);
        validateUniqueFields(volunteerRequest.getEmail(), volunteerRequest.getPhoneNumber(), volunteerId);

        BeanUtils.copyProperties(volunteerRequest, existingVolunteer, "volunteerId", "registrationDate", "availabilityStatus", "assignedIncidentId", "assignedArea", "assignedReliefRequestId");
        Volunteer updatedVolunteer = volunteerRepository.save(existingVolunteer);
        log.info("Successfully updated volunteer with ID: {}", updatedVolunteer.getVolunteerId());
        return toResponse(updatedVolunteer);
    }

    @Override
    @Transactional
    public VolunteerResponse patchVolunteer(Long volunteerId, Map<String, Object> updates) {
        log.info("Patching volunteer with ID: {}. Updates: {}", volunteerId, updates);
        Volunteer existingVolunteer = findVolunteerById(volunteerId);

        updates.forEach((key, value) -> {
            if (!SAFE_TO_PATCH_FIELDS.contains(key)) {
                log.warn("Attempted to patch a forbidden field: {} for volunteer ID: {}", key, volunteerId);
                throw new IllegalArgumentException("Field '" + key + "' cannot be updated via this endpoint.");
            }

            if ("phoneNumber".equals(key)) validateUniqueFields(null, (String) value, volunteerId);

            Field field = ReflectionUtils.findField(Volunteer.class, key);
            if (field != null) {
                field.setAccessible(true);
                ReflectionUtils.setField(field, existingVolunteer, value);
            }
        });

        Volunteer patchedVolunteer = volunteerRepository.save(existingVolunteer);
        log.info("Successfully patched volunteer with ID: {}", patchedVolunteer.getVolunteerId());
        return toResponse(patchedVolunteer);
    }

    @Override
    @Transactional
    public void deleteVolunteer(Long volunteerId) {
        log.info("Deleting volunteer with ID: {}", volunteerId);
        Volunteer volunteer = findVolunteerById(volunteerId);
        if (volunteer.getAvailabilityStatus() == AvailabilityStatus.ASSIGNED) {
            log.warn("Attempted to delete an assigned volunteer with ID: {}", volunteerId);
            throw new InvalidVolunteerStateException("Cannot delete a volunteer who is currently assigned to an incident.");
        }
        volunteerRepository.delete(volunteer);
        log.info("Successfully deleted volunteer with ID: {}", volunteerId);
    }

    @Override
    @Transactional
    public VolunteerResponse assignVolunteer(Long volunteerId, AssignmentRequest assignmentRequest) {
        log.info("Assigning volunteer {} to incident {}", volunteerId, assignmentRequest.getIncidentId());
        Volunteer volunteer = findVolunteerById(volunteerId);

        if (volunteer.getAvailabilityStatus() != AvailabilityStatus.AVAILABLE) {
            log.warn("Attempted to assign a non-available volunteer. Status: {}, ID: {}", volunteer.getAvailabilityStatus(), volunteerId);
            throw new InvalidVolunteerStateException("Volunteer cannot be assigned. Current status: " + volunteer.getAvailabilityStatus());
        }

        ensureIncidentNotAlreadyStaffed(assignmentRequest.getIncidentId(), volunteerId);

        volunteer.setAvailabilityStatus(AvailabilityStatus.ASSIGNED);
        volunteer.setAssignedIncidentId(assignmentRequest.getIncidentId());
        volunteer.setAssignedArea(assignmentRequest.getAssignedArea());
        volunteer.setAssignedReliefRequestId(assignmentRequest.getReliefRequestId());

        Volunteer assignedVolunteer = volunteerRepository.save(volunteer);
        log.info("Successfully assigned volunteer {} to incident {}", volunteerId, assignmentRequest.getIncidentId());

        linkReliefRequest(assignedVolunteer, assignmentRequest.getReliefRequestId());

        dispatchNotification(NotificationRequest.builder()
                .recipientId(assignedVolunteer.getVolunteerId())
                .recipientName(assignedVolunteer.getFirstName() + " " + assignedVolunteer.getLastName())
                .recipientEmail(assignedVolunteer.getEmail())
                .title("Volunteer Assignment")
                .message("You have been assigned to incident " + assignmentRequest.getIncidentId() + " at " + assignmentRequest.getAssignedArea() + ".")
                .notificationType(NotificationType.VOLUNTEER_ASSIGNED)
                .priority(NotificationPriority.HIGH)
                .relatedEntityId(assignmentRequest.getIncidentId())
                .relatedEntityType("INCIDENT")
                .build());

        return toResponse(assignedVolunteer);
    }

    @Override
    @Transactional
    public VolunteerResponse releaseVolunteer(Long volunteerId) {
        log.info("Releasing volunteer with ID: {}", volunteerId);
        Volunteer volunteer = findVolunteerById(volunteerId);

        if (volunteer.getAvailabilityStatus() != AvailabilityStatus.ASSIGNED) {
            log.warn("Attempted to release a volunteer who is not assigned. Status: {}, ID: {}", volunteer.getAvailabilityStatus(), volunteerId);
            throw new InvalidVolunteerStateException("Volunteer is not currently assigned and cannot be released.");
        }

        Long releasedIncidentId = volunteer.getAssignedIncidentId();
        volunteer.setAvailabilityStatus(AvailabilityStatus.AVAILABLE);
        volunteer.setAssignedIncidentId(null);
        volunteer.setAssignedArea(null);
        volunteer.setAssignedReliefRequestId(null);

        Volunteer releasedVolunteer = volunteerRepository.save(volunteer);
        log.info("Successfully released volunteer with ID: {}", volunteerId);

        unlinkReliefRequest(volunteer);

        dispatchNotification(NotificationRequest.builder()
                .recipientId(releasedVolunteer.getVolunteerId())
                .recipientName(releasedVolunteer.getFirstName() + " " + releasedVolunteer.getLastName())
                .recipientEmail(releasedVolunteer.getEmail())
                .title("Volunteer Released")
                .message("You have been released from incident " + releasedIncidentId + ". Thank you for your service.")
                .notificationType(NotificationType.VOLUNTEER_RELEASED)
                .priority(NotificationPriority.MEDIUM)
                .relatedEntityId(releasedIncidentId)
                .relatedEntityType("INCIDENT")
                .build());

        return toResponse(releasedVolunteer);
    }

    @Override
    @Transactional
    public VolunteerResponse updateAvailabilityStatus(Long volunteerId, VolunteerStatusUpdateRequest statusUpdateRequest) {
        log.info("Updating availability status for volunteer ID: {} to {}", volunteerId, statusUpdateRequest.getNewStatus());
        AvailabilityStatus newStatus = statusUpdateRequest.getNewStatus();

        if (newStatus == AvailabilityStatus.ASSIGNED) {
            throw new IllegalArgumentException("Cannot set status to ASSIGNED directly. Use the assignment API.");
        }

        Volunteer volunteer = findVolunteerById(volunteerId);

        if (volunteer.getAvailabilityStatus() == AvailabilityStatus.ASSIGNED) {
            throw new InvalidVolunteerStateException("Cannot change status while volunteer is assigned. Please release the volunteer first.");
        }

        volunteer.setAvailabilityStatus(newStatus);
        if (newStatus == AvailabilityStatus.AVAILABLE) {
            volunteer.setAssignedIncidentId(null);
            volunteer.setAssignedArea(null);
            volunteer.setAssignedReliefRequestId(null);
        }

        Volunteer updatedVolunteer = volunteerRepository.save(volunteer);
        log.info("Successfully updated availability status for volunteer ID: {}", volunteerId);

        dispatchNotification(NotificationRequest.builder()
                .recipientId(updatedVolunteer.getVolunteerId())
                .recipientName(updatedVolunteer.getFirstName() + " " + updatedVolunteer.getLastName())
                .recipientEmail(updatedVolunteer.getEmail())
                .title("Availability Updated")
                .message("Your availability status has been updated to " + newStatus + ".")
                .notificationType(NotificationType.GENERAL)
                .priority(NotificationPriority.LOW)
                .build());

        return toResponse(updatedVolunteer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VolunteerResponse> findByAvailabilityStatus(AvailabilityStatus status) {
        log.info("Finding volunteers with availability status: {}", status);
        return volunteerRepository.findByAvailabilityStatus(status).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VolunteerResponse> findBySkill(String skill) {
        log.info("Finding volunteers with skill: {}", skill);
        return volunteerRepository.findBySkillContainingIgnoreCase(skill).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VolunteerResponse> findByCity(String city) {
        log.info("Finding volunteers in city: {}", city);
        return volunteerRepository.findByCityIgnoreCase(city).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VolunteerResponse> findByAssignedIncident(Long incidentId) {
        log.info("Finding volunteers for incident ID: {}", incidentId);
        return volunteerRepository.findByAssignedIncidentId(incidentId).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VolunteerResponse> searchVolunteersByName(String name, Pageable pageable) {
        log.info("Searching for volunteers with name containing: {}", name);
        return volunteerRepository.searchByName(name, pageable).map(this::toResponse);
    }

    private Volunteer findVolunteerById(Long volunteerId) {
        return volunteerRepository.findById(volunteerId)
                .orElseThrow(() -> {
                    log.warn("Volunteer not found with ID: {}", volunteerId);
                    return new ResourceNotFoundException("Volunteer not found with ID: " + volunteerId);
                });
    }

    private void ensureIncidentNotAlreadyStaffed(Long incidentId, Long currentVolunteerId) {
        List<Volunteer> alreadyAssigned = volunteerRepository
                .findByAssignedIncidentIdAndAvailabilityStatus(incidentId, AvailabilityStatus.ASSIGNED);
        boolean conflict = alreadyAssigned.stream().anyMatch(v -> !v.getVolunteerId().equals(currentVolunteerId));
        if (conflict) {
            log.warn("Incident {} already has a volunteer assigned (BR-04)", incidentId);
            throw new InvalidVolunteerStateException(
                    "Incident " + incidentId + " already has an assigned volunteer. A single volunteer should handle the incident.");
        }
    }

    private void linkReliefRequest(Volunteer volunteer, Long reliefRequestId) {
        if (reliefRequestId == null) {
            return;
        }
        try {
            Map<String, Object> updates = new HashMap<>();
            updates.put("assignedVolunteerId", volunteer.getVolunteerId());
            updates.put("assignedVolunteerName", volunteer.getFirstName() + " " + volunteer.getLastName());
            updates.put("status", "ASSIGNED");
            reliefRequestClient.linkVolunteerToReliefRequest(reliefRequestId, updates);
            log.info("Linked volunteer {} to relief request {}", volunteer.getVolunteerId(), reliefRequestId);
        } catch (Exception e) {
            log.error("Could not link volunteer {} to relief request {}. Assignment will be rolled back: {}",
                    volunteer.getVolunteerId(), reliefRequestId, e.getMessage());
            throw e;
        }
    }

    private void unlinkReliefRequest(Volunteer volunteer) {
        Long reliefRequestId = volunteer.getAssignedReliefRequestId();
        if (reliefRequestId == null) {
            return;
        }
        try {
            Map<String, Object> updates = new HashMap<>();
            updates.put("assignedVolunteerId", null);
            updates.put("assignedVolunteerName", null);
            reliefRequestClient.linkVolunteerToReliefRequest(reliefRequestId, updates);
            log.info("Unlinked volunteer {} from relief request {}", volunteer.getVolunteerId(), reliefRequestId);
        } catch (Exception e) {
            log.error("Could not unlink volunteer {} from relief request {}. Release will be rolled back: {}",
                    volunteer.getVolunteerId(), reliefRequestId, e.getMessage());
            throw e;
        }
    }

    private void dispatchNotification(NotificationRequest request) {
        try {
            notificationClient.createNotification(request);
        } catch (Exception e) {
            log.warn("Could not dispatch notification to {}: {}", request.getRecipientEmail(), e.getMessage());
        }
    }

    private void validateUniqueFields(String email, String phoneNumber, Long currentId) {
        if (email != null) {
            volunteerRepository.findByEmail(email).ifPresent(v -> {
                if (currentId == null || !v.getVolunteerId().equals(currentId)) {
                    throw new DuplicateResourceException("Email address '" + email + "' is already in use.");
                }
            });
        }
        if (phoneNumber != null) {
            volunteerRepository.findByPhoneNumber(phoneNumber).ifPresent(v -> {
                if (currentId == null || !v.getVolunteerId().equals(currentId)) {
                    throw new DuplicateResourceException("Phone number '" + phoneNumber + "' is already in use.");
                }
            });
        }
    }

    private VolunteerResponse toResponse(Volunteer volunteer) {
        VolunteerResponse response = new VolunteerResponse();
        BeanUtils.copyProperties(volunteer, response);
        return response;
    }
}
