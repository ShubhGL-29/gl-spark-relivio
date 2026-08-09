package com.relivio.incident.entity;

import com.relivio.incident.enums.IncidentStatus;
import com.relivio.incident.enums.Severity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "incident")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long incidentId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String disasterType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Severity severity;

    @Column(nullable = false)
    @Size(max = 150)
    private String location;

    @Column(nullable = false)
    @Size(max = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus status;

    private Long reporterId;

    private String reporterName;

    private String reporterContact;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime reportedDate;

    @UpdateTimestamp
    private LocalDateTime lastUpdated;
}
