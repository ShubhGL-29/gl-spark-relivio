package com.relivio.reliefrequest.service;

import com.relivio.reliefrequest.dto.ReliefRequestRequest;
import com.relivio.reliefrequest.dto.ReliefRequestResponse;
import com.relivio.reliefrequest.enums.Priority;
import com.relivio.reliefrequest.enums.RequestStatus;

import java.util.List;
import java.util.Map;

public interface ReliefRequestService {

    ReliefRequestResponse createRequest(ReliefRequestRequest request);

    ReliefRequestResponse getRequestById(Long requestId);

    List<ReliefRequestResponse> getAllRequests();

    List<ReliefRequestResponse> getRequestsByIncidentId(Long incidentId);

    List<ReliefRequestResponse> getRequestsByStatus(RequestStatus status);

    List<ReliefRequestResponse> getRequestsByPriority(Priority priority);

    List<ReliefRequestResponse> getOpenRequests(Long incidentId);

    ReliefRequestResponse updateRequest(Long requestId, ReliefRequestRequest request);

    ReliefRequestResponse patchRequest(Long requestId, Map<String, Object> updates);

    void deleteRequest(Long requestId);
}
