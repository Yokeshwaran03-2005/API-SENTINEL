package com.apisentinel.gateway.dto;

import com.apisentinel.gateway.ApiRequest;
import com.apisentinel.gateway.AuthStatus;
import com.apisentinel.gateway.RequestVerdict;

import java.time.Instant;

/**
 * Detailed data transfer object for ApiRequest.
 */
public record ApiRequestDetailDto(
        Long id,
        String requestId,
        String httpMethod,
        String path,
        String sourceIp,
        String userAgent,
        Integer responseStatus,
        AuthStatus authStatus,
        String clientIdentifier,
        Long requestSizeBytes,
        Long responseSizeBytes,
        Long latencyMs,
        RequestVerdict verdict,
        Double threatScore,
        Long endpointId,
        String endpointName,
        Instant timestamp
) {
    public static ApiRequestDetailDto fromEntity(ApiRequest req) {
        if (req == null) return null;
        Long epId = req.getEndpoint() != null ? req.getEndpoint().getId() : null;
        String epName = req.getEndpoint() != null ? req.getEndpoint().getName() : null;
        return new ApiRequestDetailDto(
                req.getId(),
                req.getRequestId(),
                req.getHttpMethod(),
                req.getPath(),
                req.getSourceIp(),
                req.getUserAgent(),
                req.getResponseStatus(),
                req.getAuthStatus(),
                req.getClientIdentifier(),
                req.getRequestSizeBytes(),
                req.getResponseSizeBytes(),
                req.getLatencyMs(),
                req.getVerdict(),
                req.getThreatScore(),
                epId,
                epName,
                req.getTimestamp()
        );
    }
}
