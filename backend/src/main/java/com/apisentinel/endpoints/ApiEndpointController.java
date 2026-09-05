package com.apisentinel.endpoints;

import com.apisentinel.endpoints.dto.ApiEndpointDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller exposing registered API endpoints and their security configurations.
 */
@RestController
@RequestMapping("/api/endpoints")
public class ApiEndpointController {

    private final ApiEndpointService apiEndpointService;

    public ApiEndpointController(ApiEndpointService apiEndpointService) {
        this.apiEndpointService = apiEndpointService;
    }

    /**
     * GET /api/endpoints
     * Lists registered endpoints, optionally filtered by sensitivityLevel or isMonitored.
     */
    @GetMapping
    public ResponseEntity<List<ApiEndpointDto>> getEndpoints(
            @RequestParam(required = false) SensitivityLevel sensitivity,
            @RequestParam(required = false) Boolean monitored
    ) {
        List<ApiEndpointDto> endpoints = apiEndpointService.getAllEndpoints(sensitivity, monitored);
        return ResponseEntity.ok(endpoints);
    }

    /**
     * GET /api/endpoints/{id}
     * Retrieves specific endpoint configuration by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiEndpointDto> getEndpointById(@PathVariable Long id) {
        ApiEndpointDto endpoint = apiEndpointService.getEndpointById(id);
        return ResponseEntity.ok(endpoint);
    }
}
