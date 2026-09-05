package com.apisentinel.gateway.dto;

import com.apisentinel.gateway.ApiRequest;
import com.apisentinel.gateway.AuthStatus;
import com.apisentinel.gateway.RequestVerdict;

import java.time.Instant;

/**
 * Data transfer object for ApiRequest summary view.
 */
public record ApiRequestDto(
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
        Instant timestamp
) {
    public static ApiRequestDto fromEntity(ApiRequest req) {
        if (req == null) return null;
        return new ApiRequestDto(
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
                req.getTimestamp()
        );
    }
}
