package com.apisentinel.endpoints;

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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Registry of monitored API endpoints and their security configurations.
 */
@Entity
@Table(
        name = "api_endpoints",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_endpoint_path_method", columnNames = {"path", "http_method"})
        },
        indexes = {
                @Index(name = "idx_endpoint_path", columnList = "path"),
                @Index(name = "idx_endpoint_sensitivity", columnList = "sensitivity_level"),
                @Index(name = "idx_endpoint_monitored", columnList = "is_monitored")
        }
)
public class ApiEndpoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 255)
    @Column(name = "path", nullable = false, length = 255)
    private String path;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "http_method", nullable = false, length = 10)
    private HttpMethod httpMethod = HttpMethod.ANY;

    @NotBlank
    @Size(max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Size(max = 255)
    @Column(name = "description", length = 255)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "sensitivity_level", nullable = false, length = 20)
    private SensitivityLevel sensitivityLevel = SensitivityLevel.MEDIUM;

    @Column(name = "is_monitored", nullable = false)
    private boolean isMonitored = true;

    @Column(name = "requires_auth", nullable = false)
    private boolean requiresAuth = true;

    @Column(name = "max_requests_per_minute")
    private Integer maxRequestsPerMinute = 60;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ApiEndpoint() {
    }

    public ApiEndpoint(String path, HttpMethod httpMethod, String name, SensitivityLevel sensitivityLevel) {
        this.path = path;
        this.httpMethod = httpMethod != null ? httpMethod : HttpMethod.ANY;
        this.name = name;
        this.sensitivityLevel = sensitivityLevel != null ? sensitivityLevel : SensitivityLevel.MEDIUM;
        this.isMonitored = true;
        this.requiresAuth = true;
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

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(HttpMethod httpMethod) {
        this.httpMethod = httpMethod;
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

    public SensitivityLevel getSensitivityLevel() {
        return sensitivityLevel;
    }

    public void setSensitivityLevel(SensitivityLevel sensitivityLevel) {
        this.sensitivityLevel = sensitivityLevel;
    }

    public boolean isMonitored() {
        return isMonitored;
    }

    public void setMonitored(boolean monitored) {
        isMonitored = monitored;
    }

    public boolean isRequiresAuth() {
        return requiresAuth;
    }

    public void setRequiresAuth(boolean requiresAuth) {
        this.requiresAuth = requiresAuth;
    }

    public Integer getMaxRequestsPerMinute() {
        return maxRequestsPerMinute;
    }

    public void setMaxRequestsPerMinute(Integer maxRequestsPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
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
