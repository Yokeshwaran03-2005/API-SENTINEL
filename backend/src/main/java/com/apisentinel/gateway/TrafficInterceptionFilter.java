package com.apisentinel.gateway;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * High-performance API traffic interception filter.
 * Safely buffers incoming request payloads, sanitizes credentials, extracts telemetry,
 * constructs ApiRequestContext, and delegates to the SecurityPipeline.
 * Enforces policy verdicts (terminating BLOCKED and RATE_LIMITED requests with security HTTP responses).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class TrafficInterceptionFilter extends OncePerRequestFilter {

    public static final String CONTEXT_ATTRIBUTE = "API_SENTINEL_CONTEXT";
    private static final Logger log = LoggerFactory.getLogger(TrafficInterceptionFilter.class);

    private final SecurityPipeline securityPipeline;

    public TrafficInterceptionFilter(SecurityPipeline securityPipeline) {
        this.securityPipeline = securityPipeline;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Wrap request to permit repeated stream reads by security engine & controllers
        CachedBodyHttpServletRequest wrappedRequest;
        if (request instanceof CachedBodyHttpServletRequest) {
            wrappedRequest = (CachedBodyHttpServletRequest) request;
        } else {
            wrappedRequest = new CachedBodyHttpServletRequest(request);
        }

        // Extract security context
        ApiRequestContext context = buildRequestContext(wrappedRequest);

        // Bind context for request lifecycle and thread-local consumption
        wrappedRequest.setAttribute(CONTEXT_ATTRIBUTE, context);
        ApiRequestContextHolder.setContext(context);

        try {
            // Forward context to the Security Pipeline (interception -> detection -> scoring -> policy)
            InspectionResult result = securityPipeline.inspect(context);

            log.info(
                    "Intercepted {} {} from IP: {} [Status: {}, RequestId: {}, Verdict: {}]",
                    context.getHttpMethod(),
                    context.getPath(),
                    context.getSourceIp(),
                    context.getAuthStatus(),
                    context.getRequestId(),
                    result.verdict()
            );

            // Enforce policy decision: BLOCK (403 Forbidden)
            if (result.verdict() == RequestVerdict.BLOCKED) {
                writeBlockedResponse(response, context, result);
                return;
            }

            // Enforce policy decision: RATE_LIMIT (429 Too Many Requests)
            if (result.verdict() == RequestVerdict.RATE_LIMITED) {
                writeRateLimitedResponse(response, context, result);
                return;
            }

            // Add advisory header if warning level
            if (result.decision() != null && result.decision().isWarning()) {
                response.setHeader("X-Sentinel-Warning", "Elevated Threat Score: " + result.threatScore());
            }

            // Clean / Monitored / Warning traffic proceeds downstream
            filterChain.doFilter(wrappedRequest, response);
        } finally {
            ApiRequestContextHolder.clearContext();
        }
    }

    private void writeBlockedResponse(HttpServletResponse response, ApiRequestContext context, InspectionResult result) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        String reason = result.reason() != null ? result.reason() : "Request blocked by security policy";
        String escapedReason = escapeJson(reason);
        int score = result.threatScore() != null ? result.threatScore().intValue() : 0;
        String body = "{\"timestamp\":\"" + Instant.now().toString() + "\","
                + "\"status\":403,"
                + "\"error\":\"Forbidden\","
                + "\"code\":\"SECURITY_BLOCK\","
                + "\"action\":\"BLOCK\","
                + "\"threatScore\":" + score + ","
                + "\"reason\":\"" + escapedReason + "\","
                + "\"requestId\":\"" + context.getRequestId() + "\"}";
        response.getWriter().write(body);
        response.getWriter().flush();
    }

    private void writeRateLimitedResponse(HttpServletResponse response, ApiRequestContext context, InspectionResult result) throws IOException {
        response.setStatus(429); // Too Many Requests
        response.setHeader("Retry-After", "60");
        response.setContentType("application/json;charset=UTF-8");
        String reason = result.reason() != null ? result.reason() : "Rate limit exceeded";
        String escapedReason = escapeJson(reason);
        int score = result.threatScore() != null ? result.threatScore().intValue() : 0;
        String body = "{\"timestamp\":\"" + Instant.now().toString() + "\","
                + "\"status\":429,"
                + "\"error\":\"Too Many Requests\","
                + "\"code\":\"RATE_LIMIT_EXCEEDED\","
                + "\"action\":\"RATE_LIMIT\","
                + "\"threatScore\":" + score + ","
                + "\"reason\":\"" + escapedReason + "\","
                + "\"requestId\":\"" + context.getRequestId() + "\"}";
        response.getWriter().write(body);
        response.getWriter().flush();
    }

    private String escapeJson(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private ApiRequestContext buildRequestContext(CachedBodyHttpServletRequest request) {
        String requestId = resolveRequestId(request);
        String httpMethod = request.getMethod();
        String path = request.getRequestURI();
        String queryString = request.getQueryString();
        String sourceIp = resolveClientIp(request);
        Instant timestamp = Instant.now();

        Map<String, String> sanitizedHeaders = HeaderSanitizer.extractSanitizedHeaders(request);
        Map<String, String> queryParameters = extractQueryParameters(request);

        String body = "";
        long requestSizeBytes = 0;

        // Capture body for write operations with payloads
        if (shouldCaptureBody(httpMethod)) {
            body = request.getBodyAsString();
            requestSizeBytes = request.getCachedBody().length;
        } else {
            long contentLength = request.getContentLengthLong();
            requestSizeBytes = contentLength > 0 ? contentLength : 0;
        }

        AuthInfo authInfo = resolveAuthInfo(request);
        String userAgent = request.getHeader("User-Agent");

        return new ApiRequestContext(
                requestId,
                httpMethod,
                path,
                queryString,
                sourceIp,
                timestamp,
                sanitizedHeaders,
                queryParameters,
                body,
                requestSizeBytes,
                authInfo.status(),
                authInfo.scheme(),
                authInfo.clientIdentifier(),
                userAgent
        );
    }

    private boolean shouldCaptureBody(String method) {
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
    }

    private String resolveRequestId(HttpServletRequest request) {
        String headerId = request.getHeader("X-Request-ID");
        if (headerId != null && !headerId.isBlank()) {
            return headerId.trim();
        }
        return UUID.randomUUID().toString();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String[] ipHeaderCandidates = {
                "X-Forwarded-For",
                "X-Real-IP",
                "CF-Connecting-IP",
                "True-Client-IP",
                "X-Client-IP"
        };

        for (String header : ipHeaderCandidates) {
            String ipList = request.getHeader(header);
            if (ipList != null && !ipList.isBlank() && !"unknown".equalsIgnoreCase(ipList)) {
                // X-Forwarded-For may contain comma-separated IPs: client, proxy1, proxy2
                String clientIp = ipList.split(",")[0].trim();
                if (!clientIp.isEmpty()) {
                    return clientIp;
                }
            }
        }

        return request.getRemoteAddr();
    }

    private Map<String, String> extractQueryParameters(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        Enumeration<String> paramNames = request.getParameterNames();

        if (paramNames == null) {
            return Collections.emptyMap();
        }

        while (paramNames.hasMoreElements()) {
            String name = paramNames.nextElement();
            String value = request.getParameter(name);
            params.put(name, value);
        }

        return Collections.unmodifiableMap(params);
    }

    private AuthInfo resolveAuthInfo(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && !authHeader.isBlank()) {
            String trimmed = authHeader.trim();
            if (trimmed.regionMatches(true, 0, "Bearer ", 0, 7)) {
                String token = trimmed.substring(7).trim();
                String tokenSignature = token.length() > 10 ? token.substring(0, 8) + "..." : "token";
                return new AuthInfo(AuthStatus.AUTHENTICATED, "BEARER", "bearer:" + tokenSignature);
            }
            if (trimmed.regionMatches(true, 0, "Basic ", 0, 6)) {
                return new AuthInfo(AuthStatus.AUTHENTICATED, "BASIC", "basic:credentials");
            }
            return new AuthInfo(AuthStatus.AUTHENTICATED, "UNKNOWN", "auth:custom");
        }

        String apiKey = request.getHeader("X-API-Key");
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = request.getHeader("api-key");
        }

        if (apiKey != null && !apiKey.isBlank()) {
            String keyPrefix = apiKey.length() > 6 ? apiKey.substring(0, 4) + "..." : "key";
            return new AuthInfo(AuthStatus.API_KEY_VALID, "API_KEY", "key:" + keyPrefix);
        }

        return new AuthInfo(AuthStatus.ANONYMOUS, "NONE", "anonymous");
    }

    private record AuthInfo(AuthStatus status, String scheme, String clientIdentifier) {
    }
}
