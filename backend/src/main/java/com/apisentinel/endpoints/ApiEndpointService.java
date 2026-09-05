package com.apisentinel.endpoints;

import com.apisentinel.common.exception.ResourceNotFoundException;
import com.apisentinel.endpoints.dto.ApiEndpointDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service managing registered API endpoints and their security configurations.
 */
@Service
@Transactional(readOnly = true)
public class ApiEndpointService {

    private final ApiEndpointRepository apiEndpointRepository;

    public ApiEndpointService(ApiEndpointRepository apiEndpointRepository) {
        this.apiEndpointRepository = apiEndpointRepository;
    }

    /**
     * Lists all registered endpoints with optional sensitivity and monitored filters.
     */
    public List<ApiEndpointDto> getAllEndpoints(SensitivityLevel sensitivity, Boolean monitored) {
        List<ApiEndpoint> endpoints = apiEndpointRepository.findAll();

        return endpoints.stream()
                .filter(e -> sensitivity == null || e.getSensitivityLevel() == sensitivity)
                .filter(e -> monitored == null || e.isMonitored() == monitored)
                .map(ApiEndpointDto::fromEntity)
                .toList();
    }

    /**
     * Retrieves an endpoint by its unique identifier.
     */
    public ApiEndpointDto getEndpointById(Long id) {
        ApiEndpoint endpoint = apiEndpointRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ApiEndpoint", id));
        return ApiEndpointDto.fromEntity(endpoint);
    }
}
