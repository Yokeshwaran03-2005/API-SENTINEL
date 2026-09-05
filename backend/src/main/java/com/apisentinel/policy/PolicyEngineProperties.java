package com.apisentinel.policy;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configurable properties for the Policy Engine.
 */
@Component
@ConfigurationProperties(prefix = "apisentinel.policy")
public class PolicyEngineProperties {

    /**
     * Threat score threshold at or above which BLOCK is enforced (default: 80).
     */
    private int blockThreshold = 80;

    /**
     * Threat score threshold for HIGH risk actions (default: 60).
     */
    private int highThreshold = 60;

    /**
     * Threat score threshold for MEDIUM risk monitoring (default: 30).
     */
    private int mediumThreshold = 30;

    /**
     * Cooldown duration in minutes for dynamic IP blocking (default: 15 minutes).
     * Prevents permanent bans based on a single weak signal.
     */
    private int blockDurationMinutes = 15;

    /**
     * Default rate limiting time window in seconds (default: 60s).
     */
    private int rateLimitWindowSeconds = 60;

    /**
     * Default rate limiting max requests permitted per window (default: 100).
     */
    private int rateLimitMaxRequests = 100;

    /**
     * Whether to persist SecurityEvent audit records for non-clean traffic (default: true).
     */
    private boolean persistSecurityEvents = true;

    /**
     * Whether dynamic source blocking is enabled upon critical score detection (default: true).
     */
    private boolean enableDynamicBlocking = true;

    public int getBlockThreshold() {
        return blockThreshold;
    }

    public void setBlockThreshold(int blockThreshold) {
        this.blockThreshold = blockThreshold;
    }

    public int getHighThreshold() {
        return highThreshold;
    }

    public void setHighThreshold(int highThreshold) {
        this.highThreshold = highThreshold;
    }

    public int getMediumThreshold() {
        return mediumThreshold;
    }

    public void setMediumThreshold(int mediumThreshold) {
        this.mediumThreshold = mediumThreshold;
    }

    public int getBlockDurationMinutes() {
        return blockDurationMinutes;
    }

    public void setBlockDurationMinutes(int blockDurationMinutes) {
        this.blockDurationMinutes = blockDurationMinutes;
    }

    public int getRateLimitWindowSeconds() {
        return rateLimitWindowSeconds;
    }

    public void setRateLimitWindowSeconds(int rateLimitWindowSeconds) {
        this.rateLimitWindowSeconds = rateLimitWindowSeconds;
    }

    public int getRateLimitMaxRequests() {
        return rateLimitMaxRequests;
    }

    public void setRateLimitMaxRequests(int rateLimitMaxRequests) {
        this.rateLimitMaxRequests = rateLimitMaxRequests;
    }

    public boolean isPersistSecurityEvents() {
        return persistSecurityEvents;
    }

    public void setPersistSecurityEvents(boolean persistSecurityEvents) {
        this.persistSecurityEvents = persistSecurityEvents;
    }

    public boolean isEnableDynamicBlocking() {
        return enableDynamicBlocking;
    }

    public void setEnableDynamicBlocking(boolean enableDynamicBlocking) {
        this.enableDynamicBlocking = enableDynamicBlocking;
    }
}
