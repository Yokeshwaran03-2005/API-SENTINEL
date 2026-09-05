package com.apisentinel.gateway;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for ApiRequest entity.
 */
@Repository
public interface ApiRequestRepository extends JpaRepository<ApiRequest, Long>, JpaSpecificationExecutor<ApiRequest> {

    Optional<ApiRequest> findByRequestId(String requestId);

    Page<ApiRequest> findBySourceIp(String sourceIp, Pageable pageable);

    Page<ApiRequest> findByVerdict(RequestVerdict verdict, Pageable pageable);

    long countByVerdict(RequestVerdict verdict);

    List<ApiRequest> findByTimestampAfter(Instant timestamp);

    Page<ApiRequest> findByPathContainingIgnoreCase(String path, Pageable pageable);
}
