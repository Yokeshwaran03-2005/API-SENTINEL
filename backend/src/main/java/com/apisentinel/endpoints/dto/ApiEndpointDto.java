package com.apisentinel.endpoints.dto;

import com.apisentinel.endpoints.ApiEndpoint;
import com.apisentinel.endpoints.HttpMethod;
import com.apisentinel.endpoints.SensitivityLevel;

import java.time.Instant;

/**
 * Data transfer object for ApiEndpoint.
 */
public record ApiEndpointDto(
        Long id,
        String path,
        HttpMethod httpMethod,
        String name,
        String description,
        SensitivityLevel sensitivityLevel,
        boolean isMonitored,
        boolean requiresAuth,
        Integer maxRequestsPerMinute,
        Instant createdAt,
        Instant updatedAt
) {
    public static ApiEndpointDto fromEntity(ApiEndpoint endpoint) {
        if (endpoint == null) return null;
        return new ApiEndpointDto(
                endpoint.getId(),
                endpoint.getPath(),
                endpoint.getHttpMethod(),
                endpoint.getName(),
                endpoint.getDescription(),
                endpoint.getSensitivityLevel(),
                endpoint.isMonitored(),
                endpoint.isRequiresAuth(),
                endpoint.getMaxRequestsPerMinute(),
                endpoint.getCreatedAt(),
                endpoint.getUpdatedAt()
        );
    }
}
