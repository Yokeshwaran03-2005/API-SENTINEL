package com.apisentinel.detection;

import com.apisentinel.events.SecurityEvent;
import com.apisentinel.gateway.ApiRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Entity representing an individual fine-grained rule detection triggered against a request.
 */
@Entity
@Table(
        name = "threat_detections",
        indexes = {
                @Index(name = "idx_threat_det_rule", columnList = "rule_id"),
                @Index(name = "idx_threat_det_category", columnList = "detection_category"),
                @Index(name = "idx_threat_det_time", columnList = "timestamp")
        }
)
public class ThreatDetection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 64)
    @Column(name = "rule_id", nullable = false, length = 64)
    private String ruleId;

    @NotBlank
    @Size(max = 120)
    @Column(name = "rule_name", nullable = false, length = 120)
    private String ruleName;

    @NotBlank
    @Size(max = 64)
    @Column(name = "detection_category", nullable = false, length = 64)
    private String detectionCategory;

    @Column(name = "confidence_score")
    private Double confidenceScore = 1.0;

    @Size(max = 255)
    @Column(name = "matched_pattern", length = 255)
    private String matchedPattern;

    @Size(max = 100)
    @Column(name = "payload_location", length = 100)
    private String payloadLocation;

    @Column(name = "payload_snippet", columnDefinition = "TEXT")
    private String payloadSnippet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "security_event_id")
    private SecurityEvent securityEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_request_id")
    private ApiRequest apiRequest;

    @NotNull
    @Column(name = "timestamp", nullable = false, updatable = false)
    private Instant timestamp;

    public ThreatDetection() {
    }

    public ThreatDetection(String ruleId, String ruleName, String detectionCategory,
                           Double confidenceScore, String payloadSnippet) {
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.detectionCategory = detectionCategory;
        this.confidenceScore = confidenceScore;
        this.payloadSnippet = payloadSnippet;
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

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getDetectionCategory() {
        return detectionCategory;
    }

    public void setDetectionCategory(String detectionCategory) {
        this.detectionCategory = detectionCategory;
    }

    public Double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(Double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getMatchedPattern() {
        return matchedPattern;
    }

    public void setMatchedPattern(String matchedPattern) {
        this.matchedPattern = matchedPattern;
    }

    public String getPayloadLocation() {
        return payloadLocation;
    }

    public void setPayloadLocation(String payloadLocation) {
        this.payloadLocation = payloadLocation;
    }

    public String getPayloadSnippet() {
        return payloadSnippet;
    }

    public void setPayloadSnippet(String payloadSnippet) {
        this.payloadSnippet = payloadSnippet;
    }

    public SecurityEvent getSecurityEvent() {
        return securityEvent;
    }

    public void setSecurityEvent(SecurityEvent securityEvent) {
        this.securityEvent = securityEvent;
    }

    public ApiRequest getApiRequest() {
        return apiRequest;
    }

    public void setApiRequest(ApiRequest apiRequest) {
        this.apiRequest = apiRequest;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
