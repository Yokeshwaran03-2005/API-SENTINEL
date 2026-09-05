package com.apisentinel.policy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Security policy entity defining rate limits, thresholds, and enforcement rules.
 */
@Entity
@Table(
        name = "security_policies",
        indexes = {
                @Index(name = "idx_policy_name", columnList = "name", unique = true),
                @Index(name = "idx_policy_type", columnList = "policy_type"),
                @Index(name = "idx_policy_enabled", columnList = "is_enabled"),
                @Index(name = "idx_policy_priority", columnList = "priority")
        }
)
public class SecurityPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Size(max = 255)
    @Column(name = "description", length = 255)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "policy_type", nullable = false, length = 30)
    private PolicyType policyType = PolicyType.RATE_LIMIT;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "action_on_breach", nullable = false, length = 30)
    private PolicyAction actionOnBreach = PolicyAction.BLOCK;

    /**
     * Ant-style path pattern for endpoint matching (e.g. "/**", "/api/v1/auth/**").
     */
    @NotBlank
    @Size(max = 255)
    @Column(name = "endpoint_pattern", nullable = false, length = 255)
    private String endpointPattern = "/**";

    /**
     * Threshold: Maximum requests permitted within the time window.
     */
    @Column(name = "request_threshold")
    private Integer requestThreshold = 100;

    /**
     * Rate limit duration window in seconds (e.g., 60 seconds).
     */
    @Column(name = "time_window_seconds")
    private Integer timeWindowSeconds = 60;

    /**
     * Threshold: Threat score limit (0-100) above which enforcement triggers.
     */
    @Column(name = "threat_score_threshold")
    private Double threatScoreThreshold = 80.0;

    @Column(name = "is_enabled", nullable = false)
    private boolean isEnabled = true;

    @Column(name = "priority", nullable = false)
    private int priority = 100;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public SecurityPolicy() {
    }

    public SecurityPolicy(String name, PolicyType policyType, PolicyAction actionOnBreach,
                          String endpointPattern, Integer requestThreshold, Integer timeWindowSeconds) {
        this.name = name;
        this.policyType = policyType;
        this.actionOnBreach = actionOnBreach;
        this.endpointPattern = endpointPattern != null ? endpointPattern : "/**";
        this.requestThreshold = requestThreshold;
        this.timeWindowSeconds = timeWindowSeconds;
        this.isEnabled = true;
        this.priority = 100;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PolicyType getPolicyType() {
        return policyType;
    }

    public void setPolicyType(PolicyType policyType) {
        this.policyType = policyType;
    }

    public PolicyAction getActionOnBreach() {
        return actionOnBreach;
    }

    public void setActionOnBreach(PolicyAction actionOnBreach) {
        this.actionOnBreach = actionOnBreach;
    }

    public String getEndpointPattern() {
        return endpointPattern;
    }

    public void setEndpointPattern(String endpointPattern) {
        this.endpointPattern = endpointPattern;
    }

    public Integer getRequestThreshold() {
        return requestThreshold;
    }

    public void setRequestThreshold(Integer requestThreshold) {
        this.requestThreshold = requestThreshold;
    }

    public Integer getTimeWindowSeconds() {
        return timeWindowSeconds;
    }

    public void setTimeWindowSeconds(Integer timeWindowSeconds) {
        this.timeWindowSeconds = timeWindowSeconds;
    }

    public Double getThreatScoreThreshold() {
        return threatScoreThreshold;
    }

    public void setThreatScoreThreshold(Double threatScoreThreshold) {
        this.threatScoreThreshold = threatScoreThreshold;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
