package com.relivio.resource.entity;

import com.relivio.resource.enums.ResourceCategory;
import com.relivio.resource.enums.ResourceStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "resources")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long resourceId;

    @Column(nullable = false, unique = true)
    private String resourceName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceCategory category;

    @Column(nullable = false)
    private Integer quantityAvailable = 0;

    @Column(nullable = false)
    private Integer quantityAllocated = 0;

    @Column(nullable = false)
    private String unit;

    @Column(nullable = false)
    private String warehouseLocation;

    private Double latitude;
    private Double longitude;

    private LocalDate expiryDate;

    private String supplierName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
