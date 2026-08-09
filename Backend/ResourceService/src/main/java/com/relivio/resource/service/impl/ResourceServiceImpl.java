package com.relivio.resource.service.impl;

import com.relivio.resource.client.NotificationClient;
import com.relivio.resource.client.ReliefRequestClient;
import com.relivio.resource.dto.NotificationRequest;
import com.relivio.resource.dto.ResourceRequest;
import com.relivio.resource.dto.ResourceResponse;
import com.relivio.resource.entity.Resource;
import com.relivio.resource.enums.NotificationPriority;
import com.relivio.resource.enums.NotificationType;
import com.relivio.resource.enums.ResourceStatus;
import com.relivio.resource.exception.InvalidResourceStateException;
import com.relivio.resource.exception.ResourceNotFoundException;
import com.relivio.resource.repository.ResourceRepository;
import com.relivio.resource.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private static final Logger log = LoggerFactory.getLogger(ResourceServiceImpl.class);
    private static final double EARTH_RADIUS_KM = 6371.0;

    private static final Set<String> SAFE_TO_PATCH_FIELDS = Set.of(
            "category", "unit", "warehouseLocation", "latitude", "longitude", "expiryDate", "supplierName"
    );

    private final ResourceRepository resourceRepository;
    private final NotificationClient notificationClient;
    private final ReliefRequestClient reliefRequestClient;

    @Value("${app.resource.low-stock-threshold:10}")
    private Integer lowStockThreshold;

    @Override
    @Transactional
    public ResourceResponse addResource(ResourceRequest resourceRequest) {
        resourceRepository.findByResourceName(resourceRequest.getResourceName()).ifPresent(r -> {
            throw new InvalidResourceStateException("Resource with name '" + resourceRequest.getResourceName() + "' already exists.");
        });

        Resource resource = new Resource();
        BeanUtils.copyProperties(resourceRequest, resource, "quantity");
        resource.setQuantityAvailable(resourceRequest.getQuantity());
        updateResourceStatus(resource);

        Resource savedResource = resourceRepository.save(resource);
        log.info("Added new resource: {}", savedResource.getResourceName());
        return toResponse(savedResource);
    }

    @Override
    public ResourceResponse getResourceById(Long resourceId) {
        return toResponse(findResourceById(resourceId));
    }

    @Override
    public Page<ResourceResponse> getAllResources(Pageable pageable) {
        return resourceRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public ResourceResponse updateResource(Long resourceId, ResourceRequest resourceRequest) {
        Resource existingResource = findResourceById(resourceId);
        BeanUtils.copyProperties(resourceRequest, existingResource, "resourceId", "createdAt", "quantityAllocated", "status");
        updateResourceStatus(existingResource);
        return toResponse(resourceRepository.save(existingResource));
    }

    @Override
    @Transactional
    public ResourceResponse patchResource(Long resourceId, Map<String, Object> updates) {
        Resource existingResource = findResourceById(resourceId);
        updates.forEach((key, value) -> {
            if (!SAFE_TO_PATCH_FIELDS.contains(key)) {
                log.warn("Attempted to patch a forbidden field: {} for resource ID: {}", key, resourceId);
                throw new IllegalArgumentException("Field '" + key + "' cannot be updated via this endpoint.");
            }
            Field field = ReflectionUtils.findField(Resource.class, key);
            if (field != null) {
                field.setAccessible(true);
                ReflectionUtils.setField(field, existingResource, value);
            }
        });
        updateResourceStatus(existingResource);
        return toResponse(resourceRepository.save(existingResource));
    }

    @Override
    @Transactional
    public void deleteResource(Long resourceId) {
        Resource resource = findResourceById(resourceId);
        if (resource.getQuantityAllocated() > 0) {
            throw new InvalidResourceStateException("Cannot delete a resource that has been allocated.");
        }
        resourceRepository.delete(resource);
        log.info("Deleted resource with ID: {}", resourceId);
    }

    @Override
    @Transactional
    public ResourceResponse allocateResource(Long resourceId, int quantity) {
        return allocateResource(resourceId, quantity, null);
    }

    @Override
    @Transactional
    public ResourceResponse allocateResource(Long resourceId, int quantity, Long reliefRequestId) {
        if (quantity <= 0) {
            throw new InvalidResourceStateException("Allocation quantity must be positive.");
        }
        Resource resource = findResourceById(resourceId);
        if (resource.getStatus() == ResourceStatus.EXPIRED) {
            throw new InvalidResourceStateException("Cannot allocate an expired resource.");
        }
        if (resource.getQuantityAvailable() < quantity) {
            throw new InvalidResourceStateException("Not enough quantity available to allocate.");
        }
        resource.setQuantityAvailable(resource.getQuantityAvailable() - quantity);
        resource.setQuantityAllocated(resource.getQuantityAllocated() + quantity);
        updateResourceStatus(resource);
        Resource savedResource = resourceRepository.save(resource);
        log.info("Allocated {} {} of resource {} to relief request {}", quantity, savedResource.getUnit(),
                resourceId, reliefRequestId);

        linkReliefRequest(savedResource, reliefRequestId);

        dispatchNotification(NotificationRequest.builder()
                .recipientId(savedResource.getResourceId())
                .recipientName(savedResource.getResourceName())
                .title("Resource Allocated")
                .message("Allocated " + quantity + " " + savedResource.getUnit() + " of " + savedResource.getResourceName()
                        + ". Remaining available: " + savedResource.getQuantityAvailable() + ".")
                .notificationType(NotificationType.RESOURCE_ALLOCATED)
                .priority(NotificationPriority.HIGH)
                .relatedEntityId(savedResource.getResourceId())
                .relatedEntityType("RESOURCE")
                .build());

        return toResponse(savedResource);
    }

    @Override
    @Transactional
    public ResourceResponse restockResource(Long resourceId, int quantity) {
        if (quantity <= 0) {
            throw new InvalidResourceStateException("Restock quantity must be positive.");
        }
        Resource resource = findResourceById(resourceId);
        resource.setQuantityAvailable(resource.getQuantityAvailable() + quantity);
        updateResourceStatus(resource);
        Resource savedResource = resourceRepository.save(resource);
        log.info("Restocked resource {} by {} {}", resourceId, quantity, savedResource.getUnit());

        dispatchNotification(NotificationRequest.builder()
                .recipientId(savedResource.getResourceId())
                .recipientName(savedResource.getResourceName())
                .title("Resource Restocked")
                .message("Restocked " + quantity + " " + savedResource.getUnit() + " of " + savedResource.getResourceName()
                        + ". Current available quantity: " + savedResource.getQuantityAvailable() + ".")
                .notificationType(NotificationType.GENERAL)
                .priority(NotificationPriority.LOW)
                .relatedEntityId(savedResource.getResourceId())
                .relatedEntityType("RESOURCE")
                .build());

        return toResponse(savedResource);
    }

    @Override
    public List<ResourceResponse> getLowStockResources() {
        return resourceRepository.findLowStockResources(lowStockThreshold)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    public List<ResourceResponse> getExpiredResources() {
        return resourceRepository.findByExpiryDateBefore(LocalDate.now())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResourceResponse> getNearestResources(double latitude, double longitude, int limit) {
        log.info("Finding up to {} available resources nearest to ({}, {})", limit, latitude, longitude);
        List<Resource> withLocation = resourceRepository.findByLatitudeIsNotNullAndLongitudeIsNotNull();
        return withLocation.stream()
                .filter(r -> r.getQuantityAvailable() > 0)
                .sorted(Comparator.comparingDouble(r -> haversineKm(latitude, longitude, r.getLatitude(), r.getLongitude())))
                .limit(limit)
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ResourceResponse> searchResources(String name, Pageable pageable) {
        if (name == null || name.isBlank()) {
            return resourceRepository.findAll(pageable).map(this::toResponse);
        }
        return resourceRepository.findByResourceNameContainingIgnoreCase(name.trim(), pageable).map(this::toResponse);
    }

    private Resource findResourceById(Long resourceId) {
        return resourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found with ID: " + resourceId));
    }

    private void linkReliefRequest(Resource resource, Long reliefRequestId) {
        if (reliefRequestId == null) {
            return;
        }
        try {
            Map<String, Object> updates = new HashMap<>();
            updates.put("allocatedResourceId", resource.getResourceId());
            updates.put("allocatedResourceName", resource.getResourceName());
            updates.put("status", "ASSIGNED");
            reliefRequestClient.linkResourceToReliefRequest(reliefRequestId, updates);
            log.info("Linked resource {} to relief request {}", resource.getResourceId(), reliefRequestId);
        } catch (Exception e) {
            log.error("Could not link resource {} to relief request {}. Allocation will be rolled back: {}",
                    resource.getResourceId(), reliefRequestId, e.getMessage());
            throw e;
        }
    }

    private void dispatchNotification(NotificationRequest request) {
        try {
            notificationClient.createNotification(request);
        } catch (Exception e) {
            log.warn("Could not dispatch notification: {}", e.getMessage());
        }
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private void updateResourceStatus(Resource resource) {
        if (resource.getExpiryDate() != null && resource.getExpiryDate().isBefore(LocalDate.now())) {
            resource.setStatus(ResourceStatus.EXPIRED);
        } else if (resource.getQuantityAvailable() == 0) {
            resource.setStatus(ResourceStatus.OUT_OF_STOCK);
        } else if (resource.getQuantityAvailable() < lowStockThreshold) {
            resource.setStatus(ResourceStatus.LOW_STOCK);
        } else {
            resource.setStatus(ResourceStatus.AVAILABLE);
        }
    }

    private ResourceResponse toResponse(Resource resource) {
        ResourceResponse response = new ResourceResponse();
        BeanUtils.copyProperties(resource, response);
        return response;
    }
}
