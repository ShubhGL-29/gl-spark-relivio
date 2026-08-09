package com.relivio.reliefrequest.dto;

import lombok.Data;

@Data
public class IncidentResponse {
    private Long incidentId;
    private String location;
    private String title;
}
