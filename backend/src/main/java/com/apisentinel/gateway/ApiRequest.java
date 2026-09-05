package com.apisentinel.gateway;

import com.apisentinel.endpoints.ApiEndpoint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Entity logging all monitored API requests and critical security telemetry metadata.
 */
@Entity
@Table(
        name = "api_requests",
        indexes = {
                @Index(name = "idx_request_id", columnList = "request_id", unique = true),
                @Index(name = "idx_request_source_ip", columnList = "source_ip"),
                @Index(name = "idx_request_path", columnList = "path"),
                @Index(name = "idx_request_timestamp", columnList = "timestamp"),
                @Index(name = "idx_request_verdict", columnList = "verdict"),
                @Index(name = "idx_request_status", columnList = "response_status")
        }
)
public class ApiRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 64)
    @Column(name = "request_id", nullable = false, unique = true, length = 64)
    private String requestId;

    @NotBlank
    @Size(max = 10)
    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @NotBlank
    @Size(max = 500)
    @Column(name = "path", nullable = false, length = 500)
    private String path;

    @NotBlank
    @Size(max = 45)
    @Column(name = "source_ip", nullable = false, length = 45)
    private String sourceIp;

    @Size(max = 500)
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "response_status")
    private Integer responseStatus;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_status", nullable = false, length = 30)
    private AuthStatus authStatus = AuthStatus.ANONYMOUS;

    @Size(max = 100)
    @Column(name = "client_identifier", length = 100)
    private String clientIdentifier;

    @Column(name = "request_size_bytes")
    private Long requestSizeBytes = 0L;

    @Column(name = "response_size_bytes")
    private Long responseSizeBytes = 0L;

    @Column(name = "latency_ms")
    private Long latencyMs = 0L;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "verdict", nullable = false, length = 20)
    private RequestVerdict verdict = RequestVerdict.ALLOWED;

    @Column(name = "threat_score")
    private Double threatScore = 0.0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "endpoint_id")
    private ApiEndpoint endpoint;

    @NotNull
    @Column(name = "timestamp", nullable = false, updatable = false)
    private Instant timestamp;

    public ApiRequest() {
    }

    public ApiRequest(String requestId, String httpMethod, String path, String sourceIp) {
        this.requestId = requestId;
        this.httpMethod = httpMethod;
        this.path = path;
        this.sourceIp = sourceIp;
        this.authStatus = AuthStatus.ANONYMOUS;
        this.verdict = RequestVerdict.ALLOWED;
        this.threatScore = 0.0;
        this.timestamp = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = Instant.now();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getSourceIp() {
        return sourceIp;
    }

    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(Integer responseStatus) {
        this.responseStatus = responseStatus;
    }

    public AuthStatus getAuthStatus() {
        return authStatus;
    }

    public void setAuthStatus(AuthStatus authStatus) {
        this.authStatus = authStatus;
    }

    public String getClientIdentifier() {
        return clientIdentifier;
    }

    public void setClientIdentifier(String clientIdentifier) {
        this.clientIdentifier = clientIdentifier;
    }

    public Long getRequestSizeBytes() {
        return requestSizeBytes;
    }

    public void setRequestSizeBytes(Long requestSizeBytes) {
        this.requestSizeBytes = requestSizeBytes;
    }

    public Long getResponseSizeBytes() {
        return responseSizeBytes;
    }

    public void setResponseSizeBytes(Long responseSizeBytes) {
        this.responseSizeBytes = responseSizeBytes;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public RequestVerdict getVerdict() {
        return verdict;
    }

    public void setVerdict(RequestVerdict verdict) {
        this.verdict = verdict;
    }

    public Double getThreatScore() {
        return threatScore;
    }

    public void setThreatScore(Double threatScore) {
        this.threatScore = threatScore;
    }

    public ApiEndpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(ApiEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
