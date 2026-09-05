package com.apisentinel.gateway;

import com.apisentinel.common.dto.PagedResponse;
import com.apisentinel.common.exception.ResourceNotFoundException;
import com.apisentinel.gateway.dto.ApiRequestDetailDto;
import com.apisentinel.gateway.dto.ApiRequestDto;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Service managing intercepted API request telemetry and historical query logs.
 */
@Service
@Transactional(readOnly = true)
public class ApiRequestService {

    private final ApiRequestRepository apiRequestRepository;

    public ApiRequestService(ApiRequestRepository apiRequestRepository) {
        this.apiRequestRepository = apiRequestRepository;
    }

    /**
     * Lists intercepted requests matching optional multi-criteria filters with pagination.
     */
    public PagedResponse<ApiRequestDto> findRequests(
            RequestVerdict verdict,
            String sourceIp,
            String path,
            Instant startDate,
            Instant endDate,
            Pageable pageable
    ) {
        Specification<ApiRequest> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (verdict != null) {
                predicates.add(cb.equal(root.get("verdict"), verdict));
            }
            if (sourceIp != null && !sourceIp.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("sourceIp")), "%" + sourceIp.trim().toLowerCase() + "%"));
            }
            if (path != null && !path.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("path")), "%" + path.trim().toLowerCase() + "%"));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.DESC, "timestamp")
        );

        Page<ApiRequestDto> dtoPage = apiRequestRepository.findAll(spec, sortedPageable)
                .map(ApiRequestDto::fromEntity);

        return PagedResponse.fromPage(dtoPage);
    }

    /**
     * Retrieves an intercepted request by numeric ID or UUID requestId.
     */
    public ApiRequestDetailDto getRequestById(String idOrRequestId) {
        ApiRequest req = null;

        try {
            Long numericId = Long.parseLong(idOrRequestId);
            req = apiRequestRepository.findById(numericId).orElse(null);
        } catch (NumberFormatException ignored) {
        }

        if (req == null) {
            req = apiRequestRepository.findByRequestId(idOrRequestId)
                    .orElseThrow(() -> new ResourceNotFoundException("ApiRequest", idOrRequestId));
        }

        return ApiRequestDetailDto.fromEntity(req);
    }
}
