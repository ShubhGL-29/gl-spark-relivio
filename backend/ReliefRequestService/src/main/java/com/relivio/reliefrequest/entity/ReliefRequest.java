package com.relivio.reliefrequest.entity;

import com.relivio.reliefrequest.enums.Priority;
import com.relivio.reliefrequest.enums.RequestStatus;
import com.relivio.reliefrequest.enums.RequestType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "relief_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReliefRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long requestId;

    @Column(nullable = false)
    private Long incidentId;

    @Column(nullable = false, length = 100)
    private String victimName;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestType requestType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    private Long assignedVolunteerId;

    private String assignedVolunteerName;

    private Long allocatedResourceId;

    private String allocatedResourceName;

    private Long allocatedShelterId;

    private String allocatedShelterName;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime requestDate;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
