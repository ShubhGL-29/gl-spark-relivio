package com.relivio.volunteer.service;

import com.relivio.volunteer.dto.AssignmentRequest;
import com.relivio.volunteer.dto.VolunteerRequest;
import com.relivio.volunteer.dto.VolunteerResponse;
import com.relivio.volunteer.dto.VolunteerStatusUpdateRequest;
import com.relivio.volunteer.enums.AvailabilityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface VolunteerService {
    VolunteerResponse registerVolunteer(VolunteerRequest volunteerRequest);
    Page<VolunteerResponse> getAllVolunteers(Pageable pageable);
    VolunteerResponse getVolunteerById(Long volunteerId);
    VolunteerResponse updateVolunteer(Long volunteerId, VolunteerRequest volunteerRequest);
    VolunteerResponse patchVolunteer(Long volunteerId, Map<String, Object> updates);
    void deleteVolunteer(Long volunteerId);

    VolunteerResponse assignVolunteer(Long volunteerId, AssignmentRequest assignmentRequest);
    VolunteerResponse releaseVolunteer(Long volunteerId);

    VolunteerResponse updateAvailabilityStatus(Long volunteerId, VolunteerStatusUpdateRequest statusUpdateRequest);

    List<VolunteerResponse> findByAvailabilityStatus(AvailabilityStatus status);
    List<VolunteerResponse> findBySkill(String skill);
    List<VolunteerResponse> findByCity(String city);
    List<VolunteerResponse> findByAssignedIncident(Long incidentId);
    Page<VolunteerResponse> searchVolunteersByName(String name, Pageable pageable);
}
