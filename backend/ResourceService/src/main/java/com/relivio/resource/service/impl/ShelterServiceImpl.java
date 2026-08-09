package com.relivio.resource.service.impl;

import com.relivio.resource.client.NotificationClient;
import com.relivio.resource.client.ReliefRequestClient;
import com.relivio.resource.dto.NotificationRequest;
import com.relivio.resource.dto.ShelterRequest;
import com.relivio.resource.dto.ShelterResponse;
import com.relivio.resource.entity.Shelter;
import com.relivio.resource.enums.NotificationPriority;
import com.relivio.resource.enums.NotificationType;
import com.relivio.resource.exception.InvalidResourceStateException;
import com.relivio.resource.exception.ResourceNotFoundException;
import com.relivio.resource.repository.ShelterRepository;
import com.relivio.resource.service.ShelterService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShelterServiceImpl implements ShelterService {

    private static final Logger log = LoggerFactory.getLogger(ShelterServiceImpl.class);
    private static final double EARTH_RADIUS_KM = 6371.0;

    private final ShelterRepository shelterRepository;
    private final NotificationClient notificationClient;
    private final ReliefRequestClient reliefRequestClient;

    @Override
    @Transactional
    public ShelterResponse addShelter(ShelterRequest request) {
        log.info("Adding new shelter: {}", request.getName());
        shelterRepository.findByLocation(request.getLocation()).ifPresent(s -> {
            throw new InvalidResourceStateException("A shelter already exists at location '" + request.getLocation() + "'.");
        });

        Shelter shelter = new Shelter();
        BeanUtils.copyProperties(request, shelter);
        if (request.getCurrentOccupancy() != null) {
            validateOccupancyWithinCapacity(shelter, request.getCurrentOccupancy());
            shelter.setCurrentOccupancy(request.getCurrentOccupancy());
        } else {
            shelter.setCurrentOccupancy(0);
        }

        Shelter saved = shelterRepository.save(shelter);
        log.info("Added shelter with ID: {}", saved.getShelterId());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShelterResponse> getAllShelters() {
        return shelterRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ShelterResponse getShelterById(Long shelterId) {
        return toResponse(findShelterById(shelterId));
    }

    @Override
    @Transactional
    public ShelterResponse updateShelter(Long shelterId, ShelterRequest request) {
        Shelter existing = findShelterById(shelterId);
        shelterRepository.findByLocation(request.getLocation()).ifPresent(s -> {
            if (!s.getShelterId().equals(shelterId)) {
                throw new InvalidResourceStateException("A shelter already exists at location '" + request.getLocation() + "'.");
            }
        });
        BeanUtils.copyProperties(request, existing, "shelterId", "createdAt", "currentOccupancy");
        if (request.getCurrentOccupancy() != null) {
            validateOccupancyWithinCapacity(existing, request.getCurrentOccupancy());
            existing.setCurrentOccupancy(request.getCurrentOccupancy());
        }
        Shelter saved = shelterRepository.save(existing);
        log.info("Updated shelter with ID: {}", shelterId);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteShelter(Long shelterId) {
        Shelter shelter = findShelterById(shelterId);
        if (shelter.getCurrentOccupancy() > 0) {
            throw new InvalidResourceStateException("Cannot delete a shelter that is currently occupied.");
        }
        shelterRepository.delete(shelter);
        log.info("Deleted shelter with ID: {}", shelterId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShelterResponse> getSheltersWithAvailableCapacity() {
        return shelterRepository.findSheltersWithAvailableCapacity().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShelterResponse> getNearestShelters(double latitude, double longitude, int limit) {
        log.info("Finding up to {} nearest available shelters around ({}, {})", limit, latitude, longitude);
        List<Shelter> available = shelterRepository.findSheltersWithAvailableCapacity();

        return available.stream()
                .filter(s -> s.getLatitude() != null && s.getLongitude() != null)
                .sorted(Comparator.comparingDouble(s -> haversineKm(latitude, longitude, s.getLatitude(), s.getLongitude())))
                .limit(limit)
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ShelterResponse allocateShelter(Long shelterId, int numberOfPeople) {
        return allocateShelter(shelterId, numberOfPeople, null);
    }

    @Override
    @Transactional
    public ShelterResponse allocateShelter(Long shelterId, int numberOfPeople, Long reliefRequestId) {
        if (numberOfPeople <= 0) {
            throw new InvalidResourceStateException("Number of people must be positive.");
        }
        Shelter shelter = findShelterById(shelterId);
        int newOccupancy = shelter.getCurrentOccupancy() + numberOfPeople;
        validateOccupancyWithinCapacity(shelter, newOccupancy);
        shelter.setCurrentOccupancy(newOccupancy);
        Shelter saved = shelterRepository.save(shelter);
        log.info("Allocated {} people to shelter {}", numberOfPeople, shelterId);

        linkReliefRequest(saved, numberOfPeople, reliefRequestId);

        dispatchNotification(NotificationRequest.builder()
                .recipientId(saved.getShelterId())
                .recipientName(saved.getName())
                .title("Shelter Allocation")
                .message(numberOfPeople + " person(s) allocated to shelter " + saved.getName() + ". Current occupancy " + saved.getCurrentOccupancy() + "/" + saved.getCapacity() + ".")
                .notificationType(NotificationType.SHELTER_ALLOCATED)
                .priority(NotificationPriority.HIGH)
                .relatedEntityId(saved.getShelterId())
                .relatedEntityType("SHELTER")
                .build());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public ShelterResponse updateOccupancy(Long shelterId, int currentOccupancy) {
        Shelter shelter = findShelterById(shelterId);
        validateOccupancyWithinCapacity(shelter, currentOccupancy);
        shelter.setCurrentOccupancy(currentOccupancy);
        Shelter saved = shelterRepository.save(shelter);
        log.info("Updated occupancy for shelter {} to {}", shelterId, currentOccupancy);
        return toResponse(saved);
    }

    private Shelter findShelterById(Long shelterId) {
        return shelterRepository.findById(shelterId)
                .orElseThrow(() -> new ResourceNotFoundException("Shelter not found with ID: " + shelterId));
    }

    private void linkReliefRequest(Shelter shelter, int numberOfPeople, Long reliefRequestId) {
        if (reliefRequestId == null) {
            return;
        }
        try {
            Map<String, Object> updates = new HashMap<>();
            updates.put("allocatedShelterId", shelter.getShelterId());
            updates.put("allocatedShelterName", shelter.getName() + " (" + shelter.getLocation() + ")");
            updates.put("status", "ASSIGNED");
            reliefRequestClient.linkResourceToReliefRequest(reliefRequestId, updates);
            log.info("Linked shelter {} to relief request {}", shelter.getShelterId(), reliefRequestId);
        } catch (Exception e) {
            log.error("Could not link shelter {} to relief request {}. Allocation will be rolled back: {}",
                    shelter.getShelterId(), reliefRequestId, e.getMessage());
            throw e;
        }
    }

    private void validateOccupancyWithinCapacity(Shelter shelter, int occupancy) {
        if (occupancy < 0) {
            throw new InvalidResourceStateException("Occupancy cannot be negative.");
        }
        if (occupancy > shelter.getCapacity()) {
            throw new InvalidResourceStateException(
                    "Shelter capacity is " + shelter.getCapacity() + " but requested occupancy is " + occupancy + ". Please select another shelter.");
        }
    }

    private void dispatchNotification(NotificationRequest request) {
        try {
            notificationClient.createNotification(request);
        } catch (Exception e) {
            log.warn("Could not dispatch notification: {}", e.getMessage());
        }
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private ShelterResponse toResponse(Shelter shelter) {
        ShelterResponse response = new ShelterResponse();
        BeanUtils.copyProperties(shelter, response);
        response.setHasCapacity(shelter.getCurrentOccupancy() < shelter.getCapacity());
        return response;
    }
}
