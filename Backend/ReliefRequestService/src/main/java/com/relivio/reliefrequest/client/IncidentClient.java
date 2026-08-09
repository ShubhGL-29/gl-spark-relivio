package com.relivio.reliefrequest.client;

import com.relivio.reliefrequest.dto.IncidentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "incident-service")
public interface IncidentClient {
    @GetMapping("/api/incidents/{incidentId}")
    IncidentResponse getIncidentById(@PathVariable("incidentId") Long incidentId);
}
