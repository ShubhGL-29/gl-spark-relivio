package com.relivio.resource.service.impl;

import com.relivio.resource.dto.ShelterRequest;
import com.relivio.resource.dto.ShelterResponse;
import com.relivio.resource.entity.Shelter;
import com.relivio.resource.exception.InvalidResourceStateException;
import com.relivio.resource.exception.ResourceNotFoundException;
import com.relivio.resource.repository.ShelterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ShelterServiceImplTest {

    @Mock
    private ShelterRepository shelterRepository;

    @InjectMocks
    private ShelterServiceImpl shelterService;

    private Shelter shelter;
    private ShelterRequest shelterRequest;

    @BeforeEach
    void setUp() {
        shelter = Shelter.builder()
                .shelterId(1L)
                .name("Central High School")
                .location("Mumbai, Maharashtra")
                .city("Mumbai")
                .state("Maharashtra")
                .latitude(19.076)
                .longitude(72.8777)
                .capacity(200)
                .currentOccupancy(50)
                .contactNumber("02212345678")
                .build();

        shelterRequest = ShelterRequest.builder()
                .name("Central High School")
                .location("Mumbai, Maharashtra")
                .city("Mumbai")
                .state("Maharashtra")
                .latitude(19.076)
                .longitude(72.8777)
                .capacity(200)
                .currentOccupancy(50)
                .contactNumber("02212345678")
                .build();
    }

    @Test
    void addShelter_shouldReturnCreatedShelter() {
        when(shelterRepository.findByLocation("Mumbai, Maharashtra")).thenReturn(Optional.empty());
        when(shelterRepository.save(any(Shelter.class))).thenReturn(shelter);

        ShelterResponse response = shelterService.addShelter(shelterRequest);

        assertNotNull(response);
        assertEquals("Central High School", response.getName());
        assertTrue(response.isHasCapacity());
    }

    @Test
    void addShelter_shouldThrow_whenLocationExists() {
        when(shelterRepository.findByLocation("Mumbai, Maharashtra")).thenReturn(Optional.of(shelter));

        assertThrows(InvalidResourceStateException.class, () -> shelterService.addShelter(shelterRequest));
    }

    @Test
    void allocateShelter_shouldIncreaseOccupancy() {
        when(shelterRepository.findById(1L)).thenReturn(Optional.of(shelter));
        when(shelterRepository.save(any(Shelter.class))).thenReturn(shelter);

        ShelterResponse response = shelterService.allocateShelter(1L, 30);

        assertEquals(80, response.getCurrentOccupancy());
    }

    @Test
    void allocateShelter_shouldReject_whenCapacityExceeded() {
        shelter.setCurrentOccupancy(190);
        when(shelterRepository.findById(1L)).thenReturn(Optional.of(shelter));

        assertThrows(InvalidResourceStateException.class, () -> shelterService.allocateShelter(1L, 20));
    }

    @Test
    void getNearestShelters_shouldReturnOnlyAvailableSortedByProximity() {
        Shelter full = Shelter.builder()
                .shelterId(2L)
                .name("Full Shelter")
                .location("Far Away")
                .latitude(20.0)
                .longitude(73.0)
                .capacity(50)
                .currentOccupancy(50)
                .contactNumber("0000000000")
                .build();
        when(shelterRepository.findSheltersWithAvailableCapacity()).thenReturn(List.of(shelter));

        List<ShelterResponse> responses = shelterService.getNearestShelters(19.0, 72.0, 5);

        assertEquals(1, responses.size());
        assertEquals("Central High School", responses.get(0).getName());
        assertFalse(responses.stream().anyMatch(s -> s.getName().equals("Full Shelter")));
    }

    @Test
    void getShelterById_shouldThrow_whenNotFound() {
        when(shelterRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> shelterService.getShelterById(99L));
    }
}
