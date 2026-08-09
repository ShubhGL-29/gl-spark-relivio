package com.relivio.incident.dto;

import com.relivio.incident.enums.IncidentStatus;
import com.relivio.incident.enums.Severity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentResponse {

    private Long incidentId;
    private String title;
    private String disasterType;
    private Severity severity;
    private String location;
    private String description;
    private IncidentStatus status;
    private Long reporterId;
    private String reporterName;
    private String reporterContact;
    private LocalDateTime reportedDate;
    private LocalDateTime lastUpdated;
}
