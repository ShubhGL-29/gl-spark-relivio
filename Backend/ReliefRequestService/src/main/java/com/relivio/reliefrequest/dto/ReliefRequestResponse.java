package com.relivio.reliefrequest.dto;

import com.relivio.reliefrequest.enums.Priority;
import com.relivio.reliefrequest.enums.RequestStatus;
import com.relivio.reliefrequest.enums.RequestType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReliefRequestResponse {
    private Long requestId;
    private Long incidentId;
    private String victimName;
    private String phone;
    private String email;
    private RequestType requestType;
    private Priority priority;
    private String description;
    private String address;
    private RequestStatus status;
    private Long assignedVolunteerId;
    private String assignedVolunteerName;
    private Long allocatedResourceId;
    private String allocatedResourceName;
    private Long allocatedShelterId;
    private String allocatedShelterName;
    private LocalDateTime requestDate;
    private LocalDateTime updatedAt;
}
