package com.relivio.resource.service;

import com.relivio.resource.dto.ShelterRequest;
import com.relivio.resource.dto.ShelterResponse;

import java.util.List;

public interface ShelterService {
    ShelterResponse addShelter(ShelterRequest request);
    List<ShelterResponse> getAllShelters();
    ShelterResponse getShelterById(Long shelterId);
    ShelterResponse updateShelter(Long shelterId, ShelterRequest request);
    void deleteShelter(Long shelterId);
    List<ShelterResponse> getSheltersWithAvailableCapacity();
    List<ShelterResponse> getNearestShelters(double latitude, double longitude, int limit);
    ShelterResponse allocateShelter(Long shelterId, int numberOfPeople);
    ShelterResponse allocateShelter(Long shelterId, int numberOfPeople, Long reliefRequestId);
    ShelterResponse updateOccupancy(Long shelterId, int currentOccupancy);
}
