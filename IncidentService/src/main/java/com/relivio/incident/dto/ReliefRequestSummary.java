package com.relivio.incident.dto;

import com.relivio.incident.enums.RequestStatus;
import lombok.Data;

@Data
public class ReliefRequestSummary {
    private Long requestId;
    private Long incidentId;
    private RequestStatus status;
}
