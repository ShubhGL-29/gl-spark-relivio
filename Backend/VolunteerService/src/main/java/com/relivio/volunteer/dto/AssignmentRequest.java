package com.relivio.volunteer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AssignmentRequest {
    @NotNull(message = "Incident ID is required for assignment")
    private Long incidentId;

    @NotBlank(message = "Assigned area is required for assignment")
    @Size(max = 150, message = "Assigned area cannot exceed 150 characters")
    private String assignedArea;

    private Long reliefRequestId;
}
