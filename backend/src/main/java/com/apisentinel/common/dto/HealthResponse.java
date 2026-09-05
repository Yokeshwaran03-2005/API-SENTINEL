package com.apisentinel.common.dto;

/**
 * DTO representing the system health status response.
 */
public record HealthResponse(
        String status,
        String service
) {}
