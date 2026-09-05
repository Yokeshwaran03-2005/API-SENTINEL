package com.apisentinel.gateway;

import com.apisentinel.common.dto.PagedResponse;
import com.apisentinel.gateway.dto.ApiRequestDetailDto;
import com.apisentinel.gateway.dto.ApiRequestDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * REST Controller exposing intercepted API requests and traffic logs.
 */
@RestController
@RequestMapping("/api/requests")
public class ApiRequestController {

    private final ApiRequestService apiRequestService;

    public ApiRequestController(ApiRequestService apiRequestService) {
        this.apiRequestService = apiRequestService;
    }

    /**
     * GET /api/requests
     * Paginated and filterable listing of intercepted API requests.
     */
    @GetMapping
    public ResponseEntity<PagedResponse<ApiRequestDto>> getRequests(
            @RequestParam(required = false) RequestVerdict verdict,
            @RequestParam(required = false) String sourceIp,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @PageableDefault(size = 20, sort = "timestamp") Pageable pageable
    ) {
        PagedResponse<ApiRequestDto> response = apiRequestService.findRequests(
                verdict, sourceIp, path, startDate, endDate, pageable
        );
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/requests/{id}
     * Retrieves request details by ID or requestId.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiRequestDetailDto> getRequestById(@PathVariable String id) {
        ApiRequestDetailDto detail = apiRequestService.getRequestById(id);
        return ResponseEntity.ok(detail);
    }
}
