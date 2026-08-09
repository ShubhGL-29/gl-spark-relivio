package com.relivio.reliefrequest;

import com.relivio.reliefrequest.client.IncidentClient;
import com.relivio.reliefrequest.dto.IncidentResponse;
import com.relivio.reliefrequest.dto.ReliefRequestRequest;
import com.relivio.reliefrequest.dto.ReliefRequestResponse;
import com.relivio.reliefrequest.entity.ReliefRequest;
import com.relivio.reliefrequest.enums.Priority;
import com.relivio.reliefrequest.enums.RequestStatus;
import com.relivio.reliefrequest.enums.RequestType;
import com.relivio.reliefrequest.exception.InvalidRequestStateException;
import com.relivio.reliefrequest.exception.ResourceNotFoundException;
import com.relivio.reliefrequest.repository.ReliefRequestRepository;
import com.relivio.reliefrequest.service.impl.ReliefRequestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReliefRequestServiceImplTest {

    @Mock
    private ReliefRequestRepository reliefRequestRepository;

    @Mock
    private IncidentClient incidentClient;

    @InjectMocks
    private ReliefRequestServiceImpl reliefRequestService;

    private ReliefRequest reliefRequest;
    private ReliefRequestRequest requestDto;
    private IncidentResponse incidentResponse;

    @BeforeEach
    void setUp() {
        incidentResponse = new IncidentResponse();
        incidentResponse.setLocation("123 Main St");

        reliefRequest = ReliefRequest.builder()
                .requestId(1L)
                .incidentId(1L)
                .victimName("John Doe")
                .phone("1234567890")
                .email("john.doe@example.com")
                .requestType(RequestType.MEDICAL)
                .priority(Priority.HIGH)
                .description("Needs medical attention")
                .address("123 Main St")
                .status(RequestStatus.PENDING)
                .requestDate(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        requestDto = ReliefRequestRequest.builder()
                .incidentId(1L)
                .victimName("John Doe")
                .phone("1234567890")
                .email("john.doe@example.com")
                .requestType(RequestType.MEDICAL)
                .priority(Priority.HIGH)
                .description("Needs medical attention")
                .build();
    }

    @Test
    void createRequest_shouldCreateAndReturnRequest() {
        when(incidentClient.getIncidentById(anyLong())).thenReturn(incidentResponse);
        when(reliefRequestRepository.save(any(ReliefRequest.class))).thenReturn(reliefRequest);

        ReliefRequestResponse response = reliefRequestService.createRequest(requestDto);

        assertNotNull(response);
        assertEquals(requestDto.getVictimName(), response.getVictimName());
        assertEquals(RequestStatus.PENDING, response.getStatus());
        assertEquals("123 Main St", response.getAddress());
        verify(reliefRequestRepository, times(1)).save(any(ReliefRequest.class));
    }

    @Test
    void getRequestById_shouldReturnRequest_whenFound() {
        when(reliefRequestRepository.findById(1L)).thenReturn(Optional.of(reliefRequest));

        ReliefRequestResponse response = reliefRequestService.getRequestById(1L);

        assertNotNull(response);
        assertEquals(reliefRequest.getRequestId(), response.getRequestId());
    }

    @Test
    void getRequestById_shouldThrowException_whenNotFound() {
        when(reliefRequestRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> reliefRequestService.getRequestById(1L));
    }

    @Test
    void getAllRequests_shouldReturnListOfRequests() {
        when(reliefRequestRepository.findAll()).thenReturn(Collections.singletonList(reliefRequest));

        List<ReliefRequestResponse> responses = reliefRequestService.getAllRequests();

        assertNotNull(responses);
        assertEquals(1, responses.size());
    }

    @Test
    void updateRequest_shouldUpdateAndReturnRequest() {
        when(reliefRequestRepository.findById(1L)).thenReturn(Optional.of(reliefRequest));
        when(reliefRequestRepository.save(any(ReliefRequest.class))).thenReturn(reliefRequest);

        ReliefRequestResponse response = reliefRequestService.updateRequest(1L, requestDto);

        assertNotNull(response);
        assertEquals(requestDto.getDescription(), response.getDescription());
    }

    @Test
    void updateRequest_shouldThrowException_whenRequestIsFulfilled() {
        reliefRequest.setStatus(RequestStatus.FULFILLED);
        when(reliefRequestRepository.findById(1L)).thenReturn(Optional.of(reliefRequest));

        assertThrows(InvalidRequestStateException.class, () -> reliefRequestService.updateRequest(1L, requestDto));
    }

    @Test
    void deleteRequest_shouldDeleteRequest() {
        when(reliefRequestRepository.findById(1L)).thenReturn(Optional.of(reliefRequest));
        doNothing().when(reliefRequestRepository).delete(reliefRequest);

        reliefRequestService.deleteRequest(1L);

        verify(reliefRequestRepository, times(1)).delete(reliefRequest);
    }

    @Test
    void deleteRequest_shouldThrowException_whenInProgress() {
        reliefRequest.setStatus(RequestStatus.IN_PROGRESS);
        when(reliefRequestRepository.findById(1L)).thenReturn(Optional.of(reliefRequest));

        assertThrows(InvalidRequestStateException.class, () -> reliefRequestService.deleteRequest(1L));
    }

    @Test
    void updateRequest_shouldRejectFulfilled_whenNoVolunteerOrResource() {
        reliefRequest.setStatus(RequestStatus.IN_PROGRESS);
        when(reliefRequestRepository.findById(1L)).thenReturn(Optional.of(reliefRequest));

        requestDto.setStatus(RequestStatus.FULFILLED);

        assertThrows(InvalidRequestStateException.class, () -> reliefRequestService.updateRequest(1L, requestDto));
    }

    @Test
    void updateRequest_shouldAllowFulfilled_whenVolunteerAssigned() {
        reliefRequest.setStatus(RequestStatus.IN_PROGRESS);
        reliefRequest.setAssignedVolunteerId(5L);
        when(reliefRequestRepository.findById(1L)).thenReturn(Optional.of(reliefRequest));
        when(reliefRequestRepository.save(any(ReliefRequest.class))).thenReturn(reliefRequest);

        requestDto.setStatus(RequestStatus.FULFILLED);

        ReliefRequestResponse response = reliefRequestService.updateRequest(1L, requestDto);
        assertEquals(RequestStatus.FULFILLED, response.getStatus());
    }

    @Test
    void updateRequest_shouldAllowFulfilled_whenResourceAllocated() {
        reliefRequest.setStatus(RequestStatus.IN_PROGRESS);
        reliefRequest.setAllocatedResourceId(9L);
        when(reliefRequestRepository.findById(1L)).thenReturn(Optional.of(reliefRequest));
        when(reliefRequestRepository.save(any(ReliefRequest.class))).thenReturn(reliefRequest);

        requestDto.setStatus(RequestStatus.FULFILLED);

        ReliefRequestResponse response = reliefRequestService.updateRequest(1L, requestDto);
        assertEquals(RequestStatus.FULFILLED, response.getStatus());
    }

    @Test
    void patchRequest_shouldRejectInvalidTransition() {
        reliefRequest.setStatus(RequestStatus.PENDING);
        when(reliefRequestRepository.findById(1L)).thenReturn(Optional.of(reliefRequest));

        assertThrows(InvalidRequestStateException.class,
                () -> reliefRequestService.patchRequest(1L, Map.of("status", "FULFILLED")));
    }

    @Test
    void patchRequest_shouldAllowValidTransition() {
        when(reliefRequestRepository.findById(1L)).thenReturn(Optional.of(reliefRequest));
        when(reliefRequestRepository.save(any(ReliefRequest.class))).thenReturn(reliefRequest);

        ReliefRequestResponse response = reliefRequestService.patchRequest(1L, Map.of("status", "ASSIGNED"));

        assertNotNull(response);
        verify(reliefRequestRepository, times(1)).save(any(ReliefRequest.class));
    }
}
