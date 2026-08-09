package com.relivio.volunteer.repository;

import com.relivio.volunteer.entity.Volunteer;
import com.relivio.volunteer.enums.AvailabilityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VolunteerRepository extends JpaRepository<Volunteer, Long> {

    // For uniqueness validation
    Optional<Volunteer> findByEmail(String email);
    Optional<Volunteer> findByPhoneNumber(String phoneNumber);

    // Custom query methods
    List<Volunteer> findByAvailabilityStatus(AvailabilityStatus status);
    List<Volunteer> findByCityIgnoreCase(String city);
    List<Volunteer> findByAssignedIncidentId(Long incidentId);

    @Query("SELECT v FROM Volunteer v WHERE lower(v.skills) LIKE lower(concat('%', :skill, '%'))")
    List<Volunteer> findBySkillContainingIgnoreCase(String skill);

    @Query("SELECT v FROM Volunteer v WHERE lower(v.firstName) LIKE lower(concat('%', :name, '%')) OR lower(v.lastName) LIKE lower(concat('%', :name, '%'))")
    Page<Volunteer> searchByName(String name, Pageable pageable);

    List<Volunteer> findByAssignedIncidentIdAndAvailabilityStatus(Long incidentId, AvailabilityStatus status);
}
