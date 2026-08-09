package com.relivio.resource;

import com.relivio.resource.dto.ResourceRequest;
import com.relivio.resource.dto.ResourceResponse;
import com.relivio.resource.entity.Resource;
import com.relivio.resource.enums.ResourceCategory;
import com.relivio.resource.enums.ResourceStatus;
import com.relivio.resource.exception.InvalidResourceStateException;
import com.relivio.resource.exception.ResourceNotFoundException;
import com.relivio.resource.repository.ResourceRepository;
import com.relivio.resource.service.impl.ResourceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ResourceServiceImplTest {

    @Mock
    private ResourceRepository resourceRepository;

    @InjectMocks
    private ResourceServiceImpl resourceService;

    private Resource resource;
    private ResourceRequest resourceRequest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(resourceService, "lowStockThreshold", 10);

        resource = Resource.builder()
                .resourceId(1L)
                .resourceName("Water Bottle")
                .category(ResourceCategory.WATER)
                .quantityAvailable(100)
                .quantityAllocated(0)
                .unit("bottle")
                .warehouseLocation("A1")
                .status(ResourceStatus.AVAILABLE)
                .build();

        resourceRequest = ResourceRequest.builder()
                .resourceName("Water Bottle")
                .category(ResourceCategory.WATER)
                .quantity(100)
                .unit("bottle")
                .warehouseLocation("A1")
                .build();
    }

    @Test
    void addResource_shouldAddResource() {
        when(resourceRepository.findByResourceName(anyString())).thenReturn(Optional.empty());
        when(resourceRepository.save(any(Resource.class))).thenReturn(resource);

        ResourceResponse response = resourceService.addResource(resourceRequest);

        assertNotNull(response);
        assertEquals("Water Bottle", response.getResourceName());
        verify(resourceRepository, times(1)).save(any(Resource.class));
    }

    @Test
    void addResource_shouldThrowException_whenResourceExists() {
        when(resourceRepository.findByResourceName(anyString())).thenReturn(Optional.of(resource));
        assertThrows(InvalidResourceStateException.class, () -> resourceService.addResource(resourceRequest));
    }

    @Test
    void allocateResource_shouldAllocateQuantity() {
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));
        when(resourceRepository.save(any(Resource.class))).thenReturn(resource);

        ResourceResponse response = resourceService.allocateResource(1L, 20);

        assertEquals(80, response.getQuantityAvailable());
        assertEquals(20, response.getQuantityAllocated());
    }

    @Test
    void allocateResource_shouldThrowException_whenNotEnoughStock() {
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));
        assertThrows(InvalidResourceStateException.class, () -> resourceService.allocateResource(1L, 120));
    }

    @Test
    void restockResource_shouldIncreaseQuantity() {
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));
        when(resourceRepository.save(any(Resource.class))).thenReturn(resource);

        ResourceResponse response = resourceService.restockResource(1L, 50);

        assertEquals(150, response.getQuantityAvailable());
    }

    @Test
    void deleteResource_shouldDelete_whenNotAllocated() {
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));
        doNothing().when(resourceRepository).delete(any(Resource.class));
        resourceService.deleteResource(1L);
        verify(resourceRepository, times(1)).delete(any(Resource.class));
    }

    @Test
    void deleteResource_shouldThrowException_whenAllocated() {
        resource.setQuantityAllocated(10);
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));
        assertThrows(InvalidResourceStateException.class, () -> resourceService.deleteResource(1L));
    }

    @Test
    void getResourceById_shouldThrowException_whenNotFound() {
        when(resourceRepository.findById(anyLong())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> resourceService.getResourceById(1L));
    }
    
    @Test
    void updateResourceStatus_shouldSetLowStock() {
        resource.setQuantityAvailable(5);
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));
        when(resourceRepository.save(any(Resource.class))).thenReturn(resource);

        ResourceResponse response = resourceService.restockResource(1L, 1);

        assertEquals(ResourceStatus.LOW_STOCK, response.getStatus());
    }

    @Test
    void updateResourceStatus_shouldSetExpired() {
        resource.setExpiryDate(LocalDate.now().minusDays(1));
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(resource));
        when(resourceRepository.save(any(Resource.class))).thenReturn(resource);

        ResourceResponse response = resourceService.restockResource(1L, 1);

        assertEquals(ResourceStatus.EXPIRED, response.getStatus());
    }
}
