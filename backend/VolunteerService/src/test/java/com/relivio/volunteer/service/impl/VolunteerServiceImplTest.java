package com.relivio.volunteer.service.impl;

import com.relivio.volunteer.dto.AssignmentRequest;
import com.relivio.volunteer.dto.VolunteerRequest;
import com.relivio.volunteer.dto.VolunteerResponse;
import com.relivio.volunteer.entity.Volunteer;
import com.relivio.volunteer.enums.AvailabilityStatus;
import com.relivio.volunteer.exception.DuplicateResourceException;
import com.relivio.volunteer.exception.InvalidVolunteerStateException;
import com.relivio.volunteer.repository.VolunteerRepository;
import com.relivio.volunteer.service.VolunteerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VolunteerServiceImplTest {

    @Mock
    private VolunteerRepository volunteerRepository;

    @InjectMocks
    private VolunteerServiceImpl volunteerService;

    private Volunteer volunteer;
    private VolunteerRequest volunteerRequest;

    @BeforeEach
    void setUp() {
        volunteer = Volunteer.builder()
                .volunteerId(1L)
                .email("test@example.com")
                .phoneNumber("1234567890")
                .availabilityStatus(AvailabilityStatus.AVAILABLE)
                .build();

        volunteerRequest = VolunteerRequest.builder()
                .email("new.user@example.com")
                .phoneNumber("0987654321")
                .build();
    }

    @Test
    void registerVolunteer_WhenEmailExists_ShouldThrowDuplicateResourceException() {
        when(volunteerRepository.findByEmail("new.user@example.com")).thenReturn(Optional.of(new Volunteer()));
        assertThrows(DuplicateResourceException.class, () -> volunteerService.registerVolunteer(volunteerRequest));
        verify(volunteerRepository, never()).save(any());
    }

    @Test
    void assignVolunteer_WhenAvailable_ShouldSucceed() {
        when(volunteerRepository.findById(1L)).thenReturn(Optional.of(volunteer));
        when(volunteerRepository.save(any(Volunteer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssignmentRequest assignment = new AssignmentRequest();
        assignment.setIncidentId(101L);
        assignment.setAssignedArea("Sector A");

        VolunteerResponse response = volunteerService.assignVolunteer(1L, assignment);

        assertEquals(AvailabilityStatus.ASSIGNED, response.getAvailabilityStatus());
        assertEquals(101L, response.getAssignedIncidentId());
        assertEquals("Sector A", response.getAssignedArea());
        verify(volunteerRepository, times(1)).save(volunteer);
    }

    @Test
    void assignVolunteer_WhenNotAvailable_ShouldThrowInvalidVolunteerStateException() {
        volunteer.setAvailabilityStatus(AvailabilityStatus.ON_LEAVE);
        when(volunteerRepository.findById(1L)).thenReturn(Optional.of(volunteer));

        AssignmentRequest assignment = new AssignmentRequest();
        assignment.setIncidentId(101L);

        assertThrows(InvalidVolunteerStateException.class, () -> volunteerService.assignVolunteer(1L, assignment));
        verify(volunteerRepository, never()).save(any());
    }

    @Test
    void releaseVolunteer_WhenAssigned_ShouldSucceed() {
        volunteer.setAvailabilityStatus(AvailabilityStatus.ASSIGNED);
        volunteer.setAssignedIncidentId(101L);
        when(volunteerRepository.findById(1L)).thenReturn(Optional.of(volunteer));
        when(volunteerRepository.save(any(Volunteer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VolunteerResponse response = volunteerService.releaseVolunteer(1L);

        assertEquals(AvailabilityStatus.AVAILABLE, response.getAvailabilityStatus());
        assertNull(response.getAssignedIncidentId());
        assertNull(response.getAssignedArea());
        verify(volunteerRepository, times(1)).save(volunteer);
    }

    @Test
    void releaseVolunteer_WhenNotAssigned_ShouldThrowInvalidVolunteerStateException() {
        when(volunteerRepository.findById(1L)).thenReturn(Optional.of(volunteer));
        assertThrows(InvalidVolunteerStateException.class, () -> volunteerService.releaseVolunteer(1L));
        verify(volunteerRepository, never()).save(any());
    }

    @Test
    void deleteVolunteer_WhenAssigned_ShouldThrowException() {
        volunteer.setAvailabilityStatus(AvailabilityStatus.ASSIGNED);
        when(volunteerRepository.findById(1L)).thenReturn(Optional.of(volunteer));
        assertThrows(InvalidVolunteerStateException.class, () -> volunteerService.deleteVolunteer(1L));
        verify(volunteerRepository, never()).delete(any());
    }

    @Test
    void assignVolunteer_WhenIncidentAlreadyStaffed_ShouldThrowInvalidVolunteerStateException() {
        Volunteer other = Volunteer.builder()
                .volunteerId(2L)
                .availabilityStatus(AvailabilityStatus.ASSIGNED)
                .assignedIncidentId(101L)
                .build();
        when(volunteerRepository.findById(1L)).thenReturn(Optional.of(volunteer));
        when(volunteerRepository.findByAssignedIncidentIdAndAvailabilityStatus(101L, AvailabilityStatus.ASSIGNED))
                .thenReturn(java.util.List.of(other));

        AssignmentRequest assignment = new AssignmentRequest();
        assignment.setIncidentId(101L);
        assignment.setAssignedArea("Sector A");

        assertThrows(InvalidVolunteerStateException.class, () -> volunteerService.assignVolunteer(1L, assignment));
        verify(volunteerRepository, never()).save(any());
    }
}
