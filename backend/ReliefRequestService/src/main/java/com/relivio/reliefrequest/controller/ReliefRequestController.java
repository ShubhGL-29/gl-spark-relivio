package com.relivio.reliefrequest.controller;

import com.relivio.reliefrequest.dto.ReliefRequestRequest;
import com.relivio.reliefrequest.dto.ReliefRequestResponse;
import com.relivio.reliefrequest.enums.Priority;
import com.relivio.reliefrequest.enums.RequestStatus;
import com.relivio.reliefrequest.service.ReliefRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/relief-requests")
@RequiredArgsConstructor
public class ReliefRequestController {

    private final ReliefRequestService reliefRequestService;

    @PostMapping
    public ResponseEntity<ReliefRequestResponse> createRequest(@Valid @RequestBody ReliefRequestRequest request) {
        ReliefRequestResponse createdRequest = reliefRequestService.createRequest(request);
        return new ResponseEntity<>(createdRequest, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReliefRequestResponse> getRequestById(@PathVariable("id") Long requestId) {
        return ResponseEntity.ok(reliefRequestService.getRequestById(requestId));
    }

    @GetMapping
    public ResponseEntity<List<ReliefRequestResponse>> getAllRequests(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) Long incidentId,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Boolean open) {
        List<ReliefRequestResponse> requests;
        if (id != null) {
            requests = List.of(reliefRequestService.getRequestById(id));
        } else if (Boolean.TRUE.equals(open)) {
            requests = reliefRequestService.getOpenRequests(incidentId);
        } else if (incidentId != null) {
            requests = reliefRequestService.getRequestsByIncidentId(incidentId);
        } else if (status != null) {
            requests = reliefRequestService.getRequestsByStatus(status);
        } else if (priority != null) {
            requests = reliefRequestService.getRequestsByPriority(priority);
        } else {
            requests = reliefRequestService.getAllRequests();
        }
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/incident/{incidentId}")
    public ResponseEntity<List<ReliefRequestResponse>> getRequestsByIncident(@PathVariable Long incidentId) {
        return ResponseEntity.ok(reliefRequestService.getRequestsByIncidentId(incidentId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ReliefRequestResponse>> getRequestsByStatus(@PathVariable RequestStatus status) {
        return ResponseEntity.ok(reliefRequestService.getRequestsByStatus(status));
    }

    @GetMapping("/priority/{priority}")
    public ResponseEntity<List<ReliefRequestResponse>> getRequestsByPriority(@PathVariable Priority priority) {
        return ResponseEntity.ok(reliefRequestService.getRequestsByPriority(priority));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReliefRequestResponse> updateRequest(@PathVariable("id") Long requestId,
                                                               @Valid @RequestBody ReliefRequestRequest request) {
        return ResponseEntity.ok(reliefRequestService.updateRequest(requestId, request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ReliefRequestResponse> patchRequest(@PathVariable("id") Long requestId,
                                                              @RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(reliefRequestService.patchRequest(requestId, updates));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRequest(@PathVariable("id") Long requestId) {
        reliefRequestService.deleteRequest(requestId);
        return ResponseEntity.noContent().build();
    }
}
