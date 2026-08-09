package com.relivio.resource.controller;

import com.relivio.resource.dto.ResourceRequest;
import com.relivio.resource.dto.ResourceResponse;
import com.relivio.resource.service.ResourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @PostMapping
    public ResponseEntity<ResourceResponse> addResource(@Valid @RequestBody ResourceRequest resourceRequest) {
        return new ResponseEntity<>(resourceService.addResource(resourceRequest), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResourceResponse> getResourceById(@PathVariable("id") Long resourceId) {
        return ResponseEntity.ok(resourceService.getResourceById(resourceId));
    }

    @GetMapping
    public ResponseEntity<Page<ResourceResponse>> getAllResources(Pageable pageable) {
        return ResponseEntity.ok(resourceService.getAllResources(pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResourceResponse> updateResource(@PathVariable("id") Long resourceId, @Valid @RequestBody ResourceRequest resourceRequest) {
        return ResponseEntity.ok(resourceService.updateResource(resourceId, resourceRequest));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ResourceResponse> patchResource(@PathVariable("id") Long resourceId, @RequestBody Map<String, Object> updates) {
        return ResponseEntity.ok(resourceService.patchResource(resourceId, updates));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResource(@PathVariable("id") Long resourceId) {
        resourceService.deleteResource(resourceId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/allocate")
    public ResponseEntity<ResourceResponse> allocateResource(@PathVariable("id") Long resourceId, @RequestParam int quantity,
                                                             @RequestParam(required = false) Long reliefRequestId) {
        return ResponseEntity.ok(resourceService.allocateResource(resourceId, quantity, reliefRequestId));
    }

    @PostMapping("/{id}/restock")
    public ResponseEntity<ResourceResponse> restockResource(@PathVariable("id") Long resourceId, @RequestParam int quantity) {
        return ResponseEntity.ok(resourceService.restockResource(resourceId, quantity));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<ResourceResponse>> getLowStockResources() {
        return ResponseEntity.ok(resourceService.getLowStockResources());
    }

    @GetMapping("/expired")
    public ResponseEntity<List<ResourceResponse>> getExpiredResources() {
        return ResponseEntity.ok(resourceService.getExpiredResources());
    }

    @GetMapping("/search")
    public ResponseEntity<Page<ResourceResponse>> searchResources(@RequestParam String name, Pageable pageable) {
        return ResponseEntity.ok(resourceService.searchResources(name, pageable));
    }

    @GetMapping("/nearest")
    public ResponseEntity<List<ResourceResponse>> getNearestResources(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(resourceService.getNearestResources(latitude, longitude, limit));
    }
}
