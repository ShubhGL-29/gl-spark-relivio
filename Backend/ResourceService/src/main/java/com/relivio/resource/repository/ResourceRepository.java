package com.relivio.resource.repository;

import com.relivio.resource.entity.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {

    Optional<Resource> findByResourceName(String resourceName);

    Page<Resource> findByResourceNameContainingIgnoreCase(String name, Pageable pageable);

    List<Resource> findByLatitudeIsNotNullAndLongitudeIsNotNull();

    List<Resource> findByExpiryDateBefore(LocalDate date);

    @Query("SELECT r FROM Resource r WHERE r.quantityAvailable < :threshold")
    List<Resource> findLowStockResources(Integer threshold);
}
