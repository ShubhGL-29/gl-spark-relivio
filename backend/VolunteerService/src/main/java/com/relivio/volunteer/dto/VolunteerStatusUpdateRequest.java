package com.relivio.volunteer.dto;

import com.relivio.volunteer.enums.AvailabilityStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VolunteerStatusUpdateRequest {
    @NotNull(message = "Availability status cannot be null")
    private AvailabilityStatus newStatus;
}
