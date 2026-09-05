package com.apisentinel.gateway;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/**
 * Immutable context capturing security-relevant request telemetry.
 * Passed to the downstream Threat Detection Engine, Scoring Calculator,
 * and Policy Enforcement Layer.
 */
public class ApiRequestContext {

    private final String requestId;
    private final String httpMethod;
    private final String path;
    private final String queryString;
    private final String sourceIp;
    private final Instant timestamp;
    private final Map<String, String> headers;
    private final Map<String, String> queryParameters;
    private final String body;
    private final long requestSizeBytes;
    private final AuthStatus authStatus;
    private final String authScheme;
    private final String clientIdentifier;
    private final String userAgent;

    public ApiRequestContext(
            String requestId,
            String httpMethod,
            String path,
            String queryString,
            String sourceIp,
            Instant timestamp,
            Map<String, String> headers,
            Map<String, String> queryParameters,
            String body,
            long requestSizeBytes,
            AuthStatus authStatus,
            String authScheme,
            String clientIdentifier,
            String userAgent
    ) {
        this.requestId = requestId;
        this.httpMethod = httpMethod;
        this.path = path;
        this.queryString = queryString;
        this.sourceIp = sourceIp;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.headers = headers != null ? Collections.unmodifiableMap(headers) : Collections.emptyMap();
        this.queryParameters = queryParameters != null ? Collections.unmodifiableMap(queryParameters) : Collections.emptyMap();
        this.body = body != null ? body : "";
        this.requestSizeBytes = requestSizeBytes;
        this.authStatus = authStatus != null ? authStatus : AuthStatus.ANONYMOUS;
        this.authScheme = authScheme != null ? authScheme : "NONE";
        this.clientIdentifier = clientIdentifier;
        this.userAgent = userAgent;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getPath() {
        return path;
    }

    public String getQueryString() {
        return queryString;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Map<String, String> getQueryParameters() {
        return queryParameters;
    }

    public String getBody() {
        return body;
    }

    public long getRequestSizeBytes() {
        return requestSizeBytes;
    }

    public AuthStatus getAuthStatus() {
        return authStatus;
    }

    public String getAuthScheme() {
        return authScheme;
    }

    public String getClientIdentifier() {
        return clientIdentifier;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public boolean hasBody() {
        return !body.isEmpty();
    }

    @Override
    public String toString() {
        return "ApiRequestContext{" +
                "requestId='" + requestId + '\'' +
                ", method='" + httpMethod + '\'' +
                ", path='" + path + '\'' +
                ", sourceIp='" + sourceIp + '\'' +
                ", authStatus=" + authStatus +
                ", sizeBytes=" + requestSizeBytes +
                ", timestamp=" + timestamp +
                '}';
    }
}
