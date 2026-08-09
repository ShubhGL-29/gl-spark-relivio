package com.relivio.reliefrequest.dto;

import com.relivio.reliefrequest.enums.Priority;
import com.relivio.reliefrequest.enums.RequestStatus;
import com.relivio.reliefrequest.enums.RequestType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReliefRequestRequest {

    @NotNull(message = "Incident ID is mandatory")
    private Long incidentId;

    @NotBlank(message = "Victim name is mandatory")
    @Size(max = 100, message = "Victim name cannot exceed 100 characters")
    private String victimName;

    @NotBlank(message = "Phone number is mandatory")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be exactly 10 digits")
    private String phone;

    @NotBlank(message = "Email is mandatory")
    @Email(message = "Email should be in a valid format")
    private String email;

    @NotNull(message = "Request type is mandatory")
    private RequestType requestType;

    @NotNull(message = "Priority is mandatory")
    private Priority priority;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    private RequestStatus status;
}
