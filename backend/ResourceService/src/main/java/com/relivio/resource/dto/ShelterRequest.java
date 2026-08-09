package com.relivio.resource.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShelterRequest {

    @NotBlank(message = "Shelter name is mandatory")
    @Size(max = 150, message = "Shelter name cannot exceed 150 characters")
    private String name;

    @NotBlank(message = "Shelter location is mandatory")
    @Size(max = 150, message = "Shelter location cannot exceed 150 characters")
    private String location;

    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String address;

    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String city;

    @Size(max = 100, message = "State cannot exceed 100 characters")
    private String state;

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double longitude;

    @NotNull(message = "Capacity is mandatory")
    @Min(value = 1, message = "Capacity must be at least 1")
    private Integer capacity;

    @Min(value = 0, message = "Current occupancy cannot be negative")
    private Integer currentOccupancy;

    @Size(max = 500, message = "Amenities cannot exceed 500 characters")
    private String amenities;

    @NotBlank(message = "Contact number is mandatory")
    @Pattern(regexp = "^\\+?[0-9\\s().-]{7,20}$", message = "Contact number is not valid")
    @Size(max = 20, message = "Contact number cannot exceed 20 characters")
    private String contactNumber;
}
