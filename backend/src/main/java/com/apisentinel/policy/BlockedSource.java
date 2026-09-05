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
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Entity representing an IP, user, or key actively blocked by the sentinel policy engine.
 */
@Entity
@Table(
        name = "blocked_sources",
        indexes = {
                @Index(name = "idx_blocked_src_val", columnList = "source_value"),
                @Index(name = "idx_blocked_src_active", columnList = "is_active"),
                @Index(name = "idx_blocked_src_expires", columnList = "expires_at")
        }
)
public class BlockedSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 120)
    @Column(name = "source_value", nullable = false, length = 120)
    private String sourceValue;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 25)
    private SourceType sourceType = SourceType.IP_ADDRESS;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 35)
    private BlockReason reason = BlockReason.REPEATED_ATTACKS;

    @Size(max = 255)
    @Column(name = "description", length = 255)
    private String description;

    @NotNull
    @Column(name = "blocked_at", nullable = false, updatable = false)
    private Instant blockedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "is_permanent", nullable = false)
    private boolean isPermanent = false;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Size(max = 60)
    @Column(name = "created_by", length = 60)
    private String createdBy = "SYSTEM";

    public BlockedSource() {
    }

    public BlockedSource(String sourceValue, SourceType sourceType, BlockReason reason, String description,
                         Instant expiresAt, boolean isPermanent, String createdBy) {
        this.sourceValue = sourceValue;
        this.sourceType = sourceType != null ? sourceType : SourceType.IP_ADDRESS;
        this.reason = reason != null ? reason : BlockReason.REPEATED_ATTACKS;
        this.description = description;
        this.expiresAt = expiresAt;
        this.isPermanent = isPermanent;
        this.isActive = true;
        this.createdBy = createdBy != null ? createdBy : "SYSTEM";
        this.blockedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.blockedAt == null) {
            this.blockedAt = Instant.now();
        }
    }

    public boolean isExpired() {
        if (isPermanent || expiresAt == null) {
            return false;
        }
        return Instant.now().isAfter(expiresAt);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSourceValue() {
        return sourceValue;
    }

    public void setSourceValue(String sourceValue) {
        this.sourceValue = sourceValue;
    }

    public SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public BlockReason getReason() {
        return reason;
    }

    public void setReason(BlockReason reason) {
        this.reason = reason;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getBlockedAt() {
        return blockedAt;
    }

    public void setBlockedAt(Instant blockedAt) {
        this.blockedAt = blockedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isPermanent() {
        return isPermanent;
    }

    public void setPermanent(boolean permanent) {
        isPermanent = permanent;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
