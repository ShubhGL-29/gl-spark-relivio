package com.relivio.resource.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "relief-request-service")
public interface ReliefRequestClient {

    @PatchMapping("/api/relief-requests/{id}")
    void linkResourceToReliefRequest(@PathVariable("id") Long reliefRequestId,
                                     @RequestBody Map<String, Object> updates);
}
