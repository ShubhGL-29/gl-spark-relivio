package com.relivio.incident.service;

import com.relivio.incident.dto.IncidentRequest;
import com.relivio.incident.dto.IncidentResponse;
import com.relivio.incident.enums.IncidentStatus;
import com.relivio.incident.enums.Severity;

import java.util.List;
import java.util.Map;

public interface IncidentService {

    IncidentResponse createIncident(IncidentRequest incidentRequest);

    IncidentResponse getIncidentById(Long incidentId);

    List<IncidentResponse> getAllIncidents();

    IncidentResponse updateIncident(Long incidentId, IncidentRequest incidentRequest);

    void deleteIncident(Long incidentId);

    List<IncidentResponse> getIncidentsByStatus(IncidentStatus status);

    List<IncidentResponse> getIncidentsBySeverity(Severity severity);

    List<IncidentResponse> getIncidentsByDisasterType(String disasterType);

    List<IncidentResponse> getIncidentsByLocation(String location);

    IncidentResponse patchIncident(Long incidentId, Map<String, Object> updates);
}
