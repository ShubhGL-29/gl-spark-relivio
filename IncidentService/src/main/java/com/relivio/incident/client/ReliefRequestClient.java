package com.relivio.incident.client;

import com.relivio.incident.dto.ReliefRequestSummary;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "relief-request-service")
public interface ReliefRequestClient {

    @GetMapping("/api/relief-requests")
    List<ReliefRequestSummary> getReliefRequests(@RequestParam("incidentId") Long incidentId);
}
