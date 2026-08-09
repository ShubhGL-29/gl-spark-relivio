package com.relivio.resource.controller;

import com.relivio.resource.dto.ShelterRequest;
import com.relivio.resource.dto.ShelterResponse;
import com.relivio.resource.service.ShelterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shelters")
@RequiredArgsConstructor
public class ShelterController {

    private final ShelterService shelterService;

    @PostMapping
    public ResponseEntity<ShelterResponse> addShelter(@Valid @RequestBody ShelterRequest request) {
        return new ResponseEntity<>(shelterService.addShelter(request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ShelterResponse>> getAllShelters() {
        return ResponseEntity.ok(shelterService.getAllShelters());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShelterResponse> getShelterById(@PathVariable("id") Long shelterId) {
        return ResponseEntity.ok(shelterService.getShelterById(shelterId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShelterResponse> updateShelter(@PathVariable("id") Long shelterId, @Valid @RequestBody ShelterRequest request) {
        return ResponseEntity.ok(shelterService.updateShelter(shelterId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShelter(@PathVariable("id") Long shelterId) {
        shelterService.deleteShelter(shelterId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/available")
    public ResponseEntity<List<ShelterResponse>> getSheltersWithAvailableCapacity() {
        return ResponseEntity.ok(shelterService.getSheltersWithAvailableCapacity());
    }

    @GetMapping("/nearest")
    public ResponseEntity<List<ShelterResponse>> getNearestShelters(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(shelterService.getNearestShelters(latitude, longitude, limit));
    }

    @PostMapping("/{id}/allocate")
    public ResponseEntity<ShelterResponse> allocateShelter(
            @PathVariable("id") Long shelterId,
            @RequestParam int people,
            @RequestParam(required = false) Long reliefRequestId) {
        return ResponseEntity.ok(shelterService.allocateShelter(shelterId, people, reliefRequestId));
    }

    @PatchMapping("/{id}/occupancy")
    public ResponseEntity<ShelterResponse> updateOccupancy(@PathVariable("id") Long shelterId, @RequestParam int occupancy) {
        return ResponseEntity.ok(shelterService.updateOccupancy(shelterId, occupancy));
    }
}
