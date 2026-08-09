package com.relivio.resource.repository;

import com.relivio.resource.entity.Shelter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShelterRepository extends JpaRepository<Shelter, Long> {

    Optional<Shelter> findByLocation(String location);

    @Query("SELECT s FROM Shelter s WHERE s.currentOccupancy < s.capacity")
    List<Shelter> findSheltersWithAvailableCapacity();
}
