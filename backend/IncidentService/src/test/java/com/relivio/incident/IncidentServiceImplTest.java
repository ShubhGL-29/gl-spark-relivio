package com.relivio.incident;

import com.relivio.incident.client.NotificationClient;
import com.relivio.incident.client.ReliefRequestClient;
import com.relivio.incident.dto.IncidentRequest;
import com.relivio.incident.dto.IncidentResponse;
import com.relivio.incident.entity.Incident;
import com.relivio.incident.enums.IncidentStatus;
import com.relivio.incident.enums.Severity;
import com.relivio.incident.exception.InvalidIncidentStateException;
import com.relivio.incident.exception.ResourceNotFoundException;
import com.relivio.incident.repository.IncidentRepository;
import com.relivio.incident.service.IncidentServiceImpl;
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
public class IncidentServiceImplTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private ReliefRequestClient reliefRequestClient;

    @Mock
    private NotificationClient notificationClient;

    @InjectMocks
    private IncidentServiceImpl incidentService;

    private Incident incident;
    private IncidentRequest incidentRequest;

    @BeforeEach
    void setUp() {
        incident = new Incident(1L, "Test Incident", "FIRE", Severity.HIGH, "Test Location", "Test Description",
                IncidentStatus.REPORTED, 1L, "Reporter", "9999999999", LocalDateTime.now(), LocalDateTime.now());
        incidentRequest = new IncidentRequest("Test Incident", "FIRE", Severity.HIGH, "Test Location",
                "Test Description", IncidentStatus.REPORTED, 1L, "Reporter", "9999999999");
    }

    @Test
    void testCreateIncident() {
        when(incidentRepository.save(any(Incident.class))).thenReturn(incident);

        IncidentResponse result = incidentService.createIncident(incidentRequest);

        assertNotNull(result);
        assertEquals("Test Incident", result.getTitle());
        assertEquals(IncidentStatus.REPORTED, result.getStatus());
        verify(incidentRepository, times(1)).save(any(Incident.class));
    }

    @Test
    void testCreateIncident_withoutStatus_shouldDefaultToReported() {
        incidentRequest.setStatus(null);
        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IncidentResponse result = incidentService.createIncident(incidentRequest);

        assertEquals(IncidentStatus.REPORTED, result.getStatus());
    }

    @Test
    void testGetIncidentById() {
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));

        IncidentResponse result = incidentService.getIncidentById(1L);

        assertNotNull(result);
        assertEquals(incident.getIncidentId(), result.getIncidentId());
        verify(incidentRepository, times(1)).findById(1L);
    }

    @Test
    void testGetIncidentById_NotFound() {
        when(incidentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> incidentService.getIncidentById(1L));
        verify(incidentRepository, times(1)).findById(1L);
    }

    @Test
    void testGetAllIncidents() {
        when(incidentRepository.findAll()).thenReturn(Collections.singletonList(incident));

        List<IncidentResponse> result = incidentService.getAllIncidents();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(incidentRepository, times(1)).findAll();
    }

    @Test
    void testUpdateIncident() {
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any(Incident.class))).thenReturn(incident);

        IncidentResponse result = incidentService.updateIncident(1L, incidentRequest);

        assertNotNull(result);
        assertEquals("Test Incident", result.getTitle());
        verify(incidentRepository, times(1)).findById(1L);
        verify(incidentRepository, times(1)).save(any(Incident.class));
    }

    @Test
    void testUpdateIncident_invalidTransition_shouldThrow() {
        incident.setStatus(IncidentStatus.IN_PROGRESS);
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        incidentRequest.setStatus(IncidentStatus.REPORTED);

        assertThrows(InvalidIncidentStateException.class, () -> incidentService.updateIncident(1L, incidentRequest));
        verify(incidentRepository, never()).save(any(Incident.class));
    }

    @Test
    void testResolveIncident_withOpenReliefRequests_shouldThrow() {
        incident.setStatus(IncidentStatus.VERIFIED);
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(reliefRequestClient.getReliefRequests(1L))
                .thenReturn(List.of(new com.relivio.incident.dto.ReliefRequestSummary() {{
                    setRequestId(5L);
                    setIncidentId(1L);
                    setStatus(com.relivio.incident.enums.RequestStatus.IN_PROGRESS);
                }}));
        incidentRequest.setStatus(IncidentStatus.RESOLVED);

        assertThrows(InvalidIncidentStateException.class, () -> incidentService.updateIncident(1L, incidentRequest));
    }

    @Test
    void testResolveIncident_withNoOpenReliefRequests_shouldSucceed() {
        incident.setStatus(IncidentStatus.VERIFIED);
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(reliefRequestClient.getReliefRequests(1L)).thenReturn(Collections.emptyList());
        when(incidentRepository.save(any(Incident.class))).thenReturn(incident);
        incidentRequest.setStatus(IncidentStatus.RESOLVED);

        IncidentResponse result = incidentService.updateIncident(1L, incidentRequest);

        assertEquals(IncidentStatus.RESOLVED, result.getStatus());
    }

    @Test
    void testDeleteIncident() {
        when(incidentRepository.existsById(1L)).thenReturn(true);
        doNothing().when(incidentRepository).deleteById(1L);

        incidentService.deleteIncident(1L);

        verify(incidentRepository, times(1)).existsById(1L);
        verify(incidentRepository, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteIncident_NotFound() {
        when(incidentRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> incidentService.deleteIncident(1L));
        verify(incidentRepository, times(1)).existsById(1L);
        verify(incidentRepository, never()).deleteById(anyLong());
    }

    @Test
    void testPatchIncident_statusChangeShouldSucceed() {
        incident.setStatus(IncidentStatus.REPORTED);
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));

        IncidentResponse result = incidentService.patchIncident(1L, Map.of("status", "VERIFIED"));

        assertEquals(IncidentStatus.VERIFIED, result.getStatus());
    }
}
