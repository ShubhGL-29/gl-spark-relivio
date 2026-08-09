package com.relivio.incident.repository;

import com.relivio.incident.entity.Incident;
import com.relivio.incident.enums.IncidentStatus;
import com.relivio.incident.enums.Severity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    List<Incident> findByStatus(IncidentStatus status);

    List<Incident> findBySeverity(Severity severity);

    List<Incident> findByDisasterTypeIgnoreCase(String disasterType);

    List<Incident> findByLocationContainingIgnoreCase(String location);
}
