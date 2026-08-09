package com.relivio.resource.repository;

import com.relivio.resource.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {

    Optional<Resource> findByResourceName(String resourceName);

    List<Resource> findByLatitudeIsNotNullAndLongitudeIsNotNull();

    List<Resource> findByExpiryDateBefore(LocalDate date);

    @Query("SELECT r FROM Resource r WHERE r.quantityAvailable < :threshold")
    List<Resource> findLowStockResources(Integer threshold);
}
