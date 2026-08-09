package com.relivio.resource.dto;

import com.relivio.resource.enums.ResourceCategory;
import com.relivio.resource.enums.ResourceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceResponse {
    private Long resourceId;
    private String resourceName;
    private ResourceCategory category;
    private Integer quantityAvailable;
    private Integer quantityAllocated;
    private String unit;
    private String warehouseLocation;
    private Double latitude;
    private Double longitude;
    private LocalDate expiryDate;
    private String supplierName;
    private ResourceStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
