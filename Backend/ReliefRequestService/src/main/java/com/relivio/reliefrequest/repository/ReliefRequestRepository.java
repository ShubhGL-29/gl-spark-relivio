package com.relivio.reliefrequest.repository;

import com.relivio.reliefrequest.entity.ReliefRequest;
import com.relivio.reliefrequest.enums.Priority;
import com.relivio.reliefrequest.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReliefRequestRepository extends JpaRepository<ReliefRequest, Long> {
    List<ReliefRequest> findByIncidentId(Long incidentId);
    List<ReliefRequest> findByStatus(RequestStatus status);
    List<ReliefRequest> findByPriority(Priority priority);
    List<ReliefRequest> findByStatusIn(List<RequestStatus> statuses);
    List<ReliefRequest> findByIncidentIdAndStatusIn(Long incidentId, List<RequestStatus> statuses);
}
