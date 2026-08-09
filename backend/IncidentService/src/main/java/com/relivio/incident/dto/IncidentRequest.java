package com.relivio.incident.dto;

import com.relivio.incident.enums.IncidentStatus;
import com.relivio.incident.enums.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncidentRequest {

    @NotBlank(message = "Title is mandatory")
    @Size(max = 200, message = "Title cannot exceed 200 characters")
    private String title;

    @NotBlank(message = "Disaster type is mandatory")
    private String disasterType;

    @NotNull(message = "Severity is mandatory")
    private Severity severity;

    @NotBlank(message = "Location is mandatory")
    @Size(max = 150, message = "Location cannot exceed 150 characters")
    private String location;

    @NotBlank(message = "Description is mandatory")
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    private IncidentStatus status;

    private Long reporterId;

    @NotBlank(message = "Reporter name is mandatory")
    @Size(max = 100, message = "Reporter name cannot exceed 100 characters")
    private String reporterName;

    @Pattern(regexp = "^[0-9]{10}$", message = "Contact number must be exactly 10 digits")
    private String reporterContact;
}
