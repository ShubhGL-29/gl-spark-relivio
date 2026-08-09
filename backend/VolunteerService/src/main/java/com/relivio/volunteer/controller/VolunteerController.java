package com.relivio.volunteer.controller;

import com.relivio.volunteer.dto.AssignmentRequest;
import com.relivio.volunteer.dto.VolunteerRequest;
import com.relivio.volunteer.dto.VolunteerResponse;
import com.relivio.volunteer.dto.VolunteerStatusUpdateRequest;
import com.relivio.volunteer.enums.AvailabilityStatus;
import com.relivio.volunteer.service.VolunteerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/volunteers")
@RequiredArgsConstructor
public class VolunteerController {

    private final VolunteerService volunteerService;

    @PostMapping("/register")
    public ResponseEntity<VolunteerResponse> registerVolunteer(@Valid @RequestBody VolunteerRequest volunteerRequest) {
        VolunteerResponse registeredVolunteer = volunteerService.registerVolunteer(volunteerRequest);
        return new ResponseEntity<>(registeredVolunteer, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<VolunteerResponse>> getAllVolunteers(@PageableDefault(size = 10, sort = "lastName") Pageable pageable) {
        return ResponseEntity.ok(volunteerService.getAllVolunteers(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VolunteerResponse> getVolunteerById(@PathVariable("id") Long volunteerId) {
        return ResponseEntity.ok(volunteerService.getVolunteerById(volunteerId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VolunteerResponse> updateVolunteer(@PathVariable("id") Long volunteerId, @Valid @RequestBody VolunteerRequest volunteerRequest) {
        return ResponseEntity.ok(volunteerService.updateVolunteer(volunteerId, volunteerRequest));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<VolunteerResponse> patchVolunteer(@PathVariable("id") Long volunteerId, @RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(volunteerService.patchVolunteer(volunteerId, updates));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVolunteer(@PathVariable("id") Long volunteerId) {
        volunteerService.deleteVolunteer(volunteerId);
        return ResponseEntity.noContent().build();
    }

    // --- Dedicated Assignment and Status Endpoints ---

    @PostMapping("/{id}/assign")
    public ResponseEntity<VolunteerResponse> assignVolunteer(@PathVariable("id") Long volunteerId, @Valid @RequestBody AssignmentRequest assignmentRequest) {
        return ResponseEntity.ok(volunteerService.assignVolunteer(volunteerId, assignmentRequest));
    }

    @PostMapping("/{id}/release")
    public ResponseEntity<VolunteerResponse> releaseVolunteer(@PathVariable("id") Long volunteerId) {
        return ResponseEntity.ok(volunteerService.releaseVolunteer(volunteerId));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<VolunteerResponse> updateAvailabilityStatus(@PathVariable("id") Long volunteerId, @Valid @RequestBody VolunteerStatusUpdateRequest statusUpdateRequest) {
        return ResponseEntity.ok(volunteerService.updateAvailabilityStatus(volunteerId, statusUpdateRequest));
    }

    // --- Search and Filter Endpoints ---

    @GetMapping("/search")
    public ResponseEntity<Page<VolunteerResponse>> searchVolunteers(@RequestParam String name, @PageableDefault(size = 10) Pageable pageable) {
        return ResponseEntity.ok(volunteerService.searchVolunteersByName(name, pageable));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<VolunteerResponse>> getByAvailability(@PathVariable AvailabilityStatus status) {
        return ResponseEntity.ok(volunteerService.findByAvailabilityStatus(status));
    }

    @GetMapping("/skill/{skill}")
    public ResponseEntity<List<VolunteerResponse>> getBySkill(@PathVariable String skill) {
        return ResponseEntity.ok(volunteerService.findBySkill(skill));
    }

    @GetMapping("/city/{city}")
    public ResponseEntity<List<VolunteerResponse>> getByCity(@PathVariable String city) {
        return ResponseEntity.ok(volunteerService.findByCity(city));
    }

    @GetMapping("/incident/{incidentId}")
    public ResponseEntity<List<VolunteerResponse>> getByIncident(@PathVariable Long incidentId) {
        return ResponseEntity.ok(volunteerService.findByAssignedIncident(incidentId));
    }
}
