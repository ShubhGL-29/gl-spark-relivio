package com.relivio.incident.controller;

import com.relivio.incident.dto.IncidentRequest;
import com.relivio.incident.dto.IncidentResponse;
import com.relivio.incident.enums.IncidentStatus;
import com.relivio.incident.enums.Severity;
import com.relivio.incident.service.IncidentService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    @PostMapping
    public ResponseEntity<IncidentResponse> createIncident(@Valid @RequestBody IncidentRequest incidentRequest) {
        IncidentResponse createdIncident = incidentService.createIncident(incidentRequest);
        return new ResponseEntity<>(createdIncident, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentResponse> getIncidentById(@PathVariable("id") Long incidentId) {
        IncidentResponse incident = incidentService.getIncidentById(incidentId);
        return ResponseEntity.ok(incident);
    }

    @GetMapping
    public ResponseEntity<List<IncidentResponse>> getAllIncidents(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) Severity severity,
            @RequestParam(required = false) String disasterType,
            @RequestParam(required = false) String location) {
        List<IncidentResponse> incidents;
        if (id != null) {
            incidents = List.of(incidentService.getIncidentById(id));
        } else if (status != null) {
            incidents = incidentService.getIncidentsByStatus(status);
        } else if (severity != null) {
            incidents = incidentService.getIncidentsBySeverity(severity);
        } else if (StringUtils.hasText(disasterType)) {
            incidents = incidentService.getIncidentsByDisasterType(disasterType);
        } else if (StringUtils.hasText(location)) {
            incidents = incidentService.getIncidentsByLocation(location);
        } else {
            incidents = incidentService.getAllIncidents();
        }
        return ResponseEntity.ok(incidents);
    }

    @PutMapping("/{id}")
    public ResponseEntity<IncidentResponse> updateIncident(@PathVariable("id") Long incidentId, @Valid @RequestBody IncidentRequest incidentRequest) {
        IncidentResponse updatedIncident = incidentService.updateIncident(incidentId, incidentRequest);
        return ResponseEntity.ok(updatedIncident);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIncident(@PathVariable("id") Long incidentId) {
        incidentService.deleteIncident(incidentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<IncidentResponse>> getIncidentsByStatus(@PathVariable("status") IncidentStatus status) {
        List<IncidentResponse> incidents = incidentService.getIncidentsByStatus(status);
        return ResponseEntity.ok(incidents);
    }

    @GetMapping("/severity/{severity}")
    public ResponseEntity<List<IncidentResponse>> getIncidentsBySeverity(@PathVariable("severity") Severity severity) {
        List<IncidentResponse> incidents = incidentService.getIncidentsBySeverity(severity);
        return ResponseEntity.ok(incidents);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<IncidentResponse> patchIncident(@PathVariable("id") Long incidentId, @RequestBody Map<String, Object> updates) {
        IncidentResponse patchedIncident = incidentService.patchIncident(incidentId, updates);
        return ResponseEntity.ok(patchedIncident);
    }
}
