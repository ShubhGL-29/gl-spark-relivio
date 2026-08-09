package com.relivio.resource.service;

import com.relivio.resource.dto.ResourceRequest;
import com.relivio.resource.dto.ResourceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface ResourceService {
    ResourceResponse addResource(ResourceRequest resourceRequest);
    ResourceResponse getResourceById(Long resourceId);
    Page<ResourceResponse> getAllResources(Pageable pageable);
    ResourceResponse updateResource(Long resourceId, ResourceRequest resourceRequest);
    ResourceResponse patchResource(Long resourceId, Map<String, Object> updates);
    void deleteResource(Long resourceId);
    ResourceResponse allocateResource(Long resourceId, int quantity);
    ResourceResponse allocateResource(Long resourceId, int quantity, Long reliefRequestId);
    ResourceResponse restockResource(Long resourceId, int quantity);
    List<ResourceResponse> getLowStockResources();
    List<ResourceResponse> getExpiredResources();
    List<ResourceResponse> getNearestResources(double latitude, double longitude, int limit);
    Page<ResourceResponse> searchResources(String name, Pageable pageable);
}
