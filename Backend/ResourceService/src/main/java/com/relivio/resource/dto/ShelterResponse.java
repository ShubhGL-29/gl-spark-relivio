package com.relivio.resource.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShelterResponse {
    private Long shelterId;
    private String name;
    private String location;
    private String address;
    private String city;
    private String state;
    private Double latitude;
    private Double longitude;
    private Integer capacity;
    private Integer currentOccupancy;
    private boolean hasCapacity;
    private String amenities;
    private String contactNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
