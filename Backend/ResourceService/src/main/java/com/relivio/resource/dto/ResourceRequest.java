package com.relivio.resource.dto;

import com.relivio.resource.enums.ResourceCategory;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceRequest {

    @NotBlank(message = "Resource name is mandatory")
    @Size(max = 150, message = "Resource name cannot exceed 150 characters")
    private String resourceName;

    @NotNull(message = "Category is mandatory")
    private ResourceCategory category;

    @NotNull(message = "Quantity is mandatory")
    @Min(value = 0, message = "Quantity cannot be negative")
    private Integer quantity;

    @NotBlank(message = "Unit is mandatory")
    @Size(max = 50, message = "Unit cannot exceed 50 characters")
    private String unit;

    @NotBlank(message = "Warehouse location is mandatory")
    @Size(max = 150, message = "Warehouse location cannot exceed 150 characters")
    private String warehouseLocation;

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double longitude;

    @FutureOrPresent(message = "Expiry date must be in the present or future")
    private LocalDate expiryDate;

    @Size(max = 150, message = "Supplier name cannot exceed 150 characters")
    private String supplierName;
}
