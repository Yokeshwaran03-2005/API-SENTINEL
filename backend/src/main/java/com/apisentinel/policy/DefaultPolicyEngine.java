package com.apisentinel.policy;

import com.apisentinel.events.*;
import com.apisentinel.gateway.ApiRequestContext;
import com.apisentinel.scoring.RiskLevel;
import com.apisentinel.scoring.ThreatCategory;
import com.apisentinel.scoring.ThreatScoreResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

/**
 * Default implementation of PolicyEngine.
 * Converts ThreatScoreResult into an authoritative SecurityDecision,
 * enforcing source blocking, rate limits, endpoint policies, and event logging.
 */
@Service
public class DefaultPolicyEngine implements PolicyEngine {

    private static final Logger log = LoggerFactory.getLogger(DefaultPolicyEngine.class);

    private final PolicyEngineProperties properties;
    private final BlockedSourceRepository blockedSourceRepository;
    private final SecurityPolicyRepository securityPolicyRepository;
    private final SecurityEventRepository securityEventRepository;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // Fast in-memory tracking of sliding-window requests for rate limiting
    private final ConcurrentHashMap<String, Deque<Long>> rateLimitWindows = new ConcurrentHashMap<>();

    // Fast in-memory cache for blocked sources (source -> expiry Instant)
    private final ConcurrentHashMap<String, Instant> activeBlockedSources = new ConcurrentHashMap<>();

    public DefaultPolicyEngine(
            PolicyEngineProperties properties,
            BlockedSourceRepository blockedSourceRepository,
            SecurityPolicyRepository securityPolicyRepository,
            SecurityEventRepository securityEventRepository
    ) {
        this.properties = properties != null ? properties : new PolicyEngineProperties();
        this.blockedSourceRepository = blockedSourceRepository;
        this.securityPolicyRepository = securityPolicyRepository;
        this.securityEventRepository = securityEventRepository;
    }

    @Override
    public SecurityDecision evaluate(ApiRequestContext context, ThreatScoreResult scoreResult) {
        String sourceIp = context.getSourceIp();
        String path = context.getPath();
        int score = scoreResult != null ? scoreResult.totalScore() : 0;
        RiskLevel riskLevel = scoreResult != null ? scoreResult.riskLevel() : RiskLevel.LOW;

        // 1. Source Blocking Check: Is this source IP already actively blocked?
        if (isSourceBlocked(sourceIp)) {
            String reason = "Source IP [" + sourceIp + "] is actively blocked by security policy.";
            log.warn("Blocked source access attempt: {} on {}", sourceIp, path);
            recordSecurityEvent(context, ThreatType.UNKNOWN, ThreatSeverity.CRITICAL, (double) Math.max(score, 100),
                    reason, MitigationAction.BLOCKED, null);
            return SecurityDecision.block(reason, Math.max(score, 100), RiskLevel.CRITICAL);
        }

        // 2. Endpoint-Specific Policy Checks (from Database or Config)
        Optional<SecurityDecision> endpointDecision = evaluateEndpointPolicies(context, score, riskLevel);
        if (endpointDecision.isPresent()) {
            SecurityDecision decision = endpointDecision.get();
            if (decision.isBlocked()) {
                handleDynamicSourceBlock(sourceIp, decision.reason(), score);
                recordSecurityEvent(context, ThreatType.UNKNOWN, ThreatSeverity.CRITICAL, (double) score,
                        decision.reason(), MitigationAction.BLOCKED, null);
                return decision;
            }
        }

        // 3. Rate Limiting Policy Check (Sliding Window per Source & Endpoint)
        if (isRateLimited(sourceIp, path)) {
            String rateLimitReason = "Abnormal request frequency: rate limit exceeded on endpoint " + path;
            log.warn("Rate limit breached by {} on {}", sourceIp, path);
            recordSecurityEvent(context, ThreatType.RATE_LIMIT_EXCEEDED, ThreatSeverity.HIGH, (double) Math.max(score, 65),
                    rateLimitReason, MitigationAction.RATE_LIMITED, null);
            return SecurityDecision.rateLimit(rateLimitReason, Math.max(score, 65), RiskLevel.HIGH);
        }

        // 4. Threat Score Threshold Evaluation
        String decisionReason = formatDecisionReason(scoreResult);

        if (score >= properties.getBlockThreshold()) {
            // CRITICAL Risk (>= 80): Immediate BLOCK
            // Dynamically register temporary block cooldown to protect against ongoing attack
            handleDynamicSourceBlock(sourceIp, decisionReason, score);

            recordSecurityEvent(context, resolvePrimaryThreatType(scoreResult), ThreatSeverity.CRITICAL,
                    (double) score, decisionReason, MitigationAction.BLOCKED, extractEvidence(scoreResult));

            log.warn("Decision: BLOCK for {} on {} (Score: {}, Reason: {})", sourceIp, path, score, decisionReason);
            return SecurityDecision.block(decisionReason, score, RiskLevel.CRITICAL);

        } else if (score >= properties.getHighThreshold()) {
            // HIGH Risk (60 - 79): RATE_LIMIT if rate abuse involved, otherwise WARN
            boolean hasRateAbuse = hasThreatCategory(scoreResult, ThreatCategory.RATE_ABUSE);
            PolicyAction action = hasRateAbuse ? PolicyAction.RATE_LIMIT : PolicyAction.WARN;
            MitigationAction mitAction = hasRateAbuse ? MitigationAction.RATE_LIMITED : MitigationAction.ALERT_ONLY;

            recordSecurityEvent(context, resolvePrimaryThreatType(scoreResult), ThreatSeverity.HIGH,
                    (double) score, decisionReason, mitAction, extractEvidence(scoreResult));

            log.warn("Decision: {} for {} on {} (Score: {}, Reason: {})", action, sourceIp, path, score, decisionReason);
            return new SecurityDecision(action, decisionReason, score, RiskLevel.HIGH);

        } else if (score >= properties.getMediumThreshold()) {
            // MEDIUM Risk (30 - 59): MONITOR (audit log created, request allowed)
            recordSecurityEvent(context, resolvePrimaryThreatType(scoreResult), ThreatSeverity.MEDIUM,
                    (double) score, decisionReason, MitigationAction.ALLOWED, extractEvidence(scoreResult));

            log.info("Decision: MONITOR for {} on {} (Score: {}, Reason: {})", sourceIp, path, score, decisionReason);
            return SecurityDecision.monitor(decisionReason, score, RiskLevel.MEDIUM);

        } else {
            // LOW Risk (0 - 29): ALLOW
            String cleanReason = score > 0 ? decisionReason : "Normal request within baseline safety thresholds.";
            if (score > 0) {
                recordSecurityEvent(context, resolvePrimaryThreatType(scoreResult), ThreatSeverity.LOW,
                        (double) score, cleanReason, MitigationAction.ALLOWED, extractEvidence(scoreResult));
            }
            log.debug("Decision: ALLOW for {} on {} (Score: {})", sourceIp, path, score);
            return SecurityDecision.allow(cleanReason, score, RiskLevel.LOW);
        }
    }

    @Override
    public boolean isSourceBlocked(String sourceValue) {
        if (sourceValue == null || sourceValue.isBlank()) {
            return false;
        }

        Instant now = Instant.now();

        // 1. Check in-memory cache
        Instant cachedExpiry = activeBlockedSources.get(sourceValue);
        if (cachedExpiry != null) {
            if (now.isBefore(cachedExpiry)) {
                return true;
            } else {
                activeBlockedSources.remove(sourceValue);
            }
        }

        // 2. Check persistence repository if available
        if (blockedSourceRepository != null) {
            try {
                Optional<BlockedSource> blockedOpt = blockedSourceRepository.findBySourceValueAndIsActiveTrue(sourceValue);
                if (blockedOpt.isPresent()) {
                    BlockedSource blocked = blockedOpt.get();
                    if (!blocked.isExpired()) {
                        activeBlockedSources.put(sourceValue, blocked.getExpiresAt() != null ? blocked.getExpiresAt() : now.plus(Duration.ofDays(365)));
                        return true;
                    } else {
                        blocked.setActive(false);
                        blockedSourceRepository.save(blocked);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to query blocked_sources repository: {}", e.getMessage());
            }
        }

        return false;
    }

    @Override
    public void blockSource(String sourceValue, String reason, int durationMinutes) {
        if (sourceValue == null || sourceValue.isBlank()) {
            return;
        }

        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(durationMinutes > 0 ? durationMinutes : properties.getBlockDurationMinutes()));
        activeBlockedSources.put(sourceValue, expiresAt);

        if (blockedSourceRepository != null) {
            try {
                BlockedSource blocked = new BlockedSource(
                        sourceValue,
                        SourceType.IP_ADDRESS,
                        BlockReason.HIGH_THREAT_SCORE,
                        reason,
                        expiresAt,
                        false, // Never permanent on automated scoring to protect legitimate users
                        "POLICY_ENGINE"
                );
                blockedSourceRepository.save(blocked);
                log.info("Persisted BlockedSource for IP: {} (expires: {})", sourceValue, expiresAt);
            } catch (Exception e) {
                log.error("Failed to persist BlockedSource: {}", e.getMessage());
            }
        }
    }

    @Override
    public void unblockSource(String sourceValue) {
        if (sourceValue == null) return;
        activeBlockedSources.remove(sourceValue);
        if (blockedSourceRepository != null) {
            try {
                blockedSourceRepository.findBySourceValueAndIsActiveTrue(sourceValue).ifPresent(b -> {
                    b.setActive(false);
                    blockedSourceRepository.save(b);
                });
            } catch (Exception e) {
                log.error("Failed to unblock source in repository: {}", e.getMessage());
            }
        }
    }

    /**
     * Sliding window rate limiter checking request frequency per source IP.
     */
    public boolean isRateLimited(String sourceIp, String path) {
        if (sourceIp == null) {
            return false;
        }

        String key = sourceIp + ":" + (path != null ? path : "");
        long now = System.currentTimeMillis();
        long windowMillis = properties.getRateLimitWindowSeconds() * 1000L;
        int maxRequests = properties.getRateLimitMaxRequests();

        Deque<Long> timestamps = rateLimitWindows.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());

        // Evict expired timestamps outside the sliding window
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && (now - timestamps.peekFirst()) > windowMillis) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= maxRequests) {
                return true;
            }

            timestamps.addLast(now);
            return false;
        }
    }

    /**
     * Evaluates endpoint security policies defined in the database.
     */
    private Optional<SecurityDecision> evaluateEndpointPolicies(ApiRequestContext context, int score, RiskLevel riskLevel) {
        if (securityPolicyRepository == null) {
            return Optional.empty();
        }

        try {
            List<SecurityPolicy> activePolicies = securityPolicyRepository.findByIsEnabledTrueOrderByPriorityAsc();
            for (SecurityPolicy policy : activePolicies) {
                if (pathMatcher.match(policy.getEndpointPattern(), context.getPath())) {
                    Double threshold = policy.getThreatScoreThreshold();
                    if (threshold != null && score >= threshold) {
                        String reason = "Endpoint policy [" + policy.getName() + "] breached: score "
                                + score + " exceeds threshold " + threshold;
                        PolicyAction action = policy.getActionOnBreach();
                        return Optional.of(new SecurityDecision(action, reason, score, riskLevel));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to evaluate endpoint security policies: {}", e.getMessage());
        }

        return Optional.empty();
    }

    private void handleDynamicSourceBlock(String sourceIp, String reason, int score) {
        if (!properties.isEnableDynamicBlocking()) {
            return;
        }
        // Protect legitimate users: only block if score meets or exceeds blockThreshold (e.g. 80)
        if (score >= properties.getBlockThreshold()) {
            blockSource(sourceIp, reason, properties.getBlockDurationMinutes());
        }
    }

    private void recordSecurityEvent(
            ApiRequestContext context,
            ThreatType threatType,
            ThreatSeverity severity,
            Double score,
            String reason,
            MitigationAction action,
            String evidence
    ) {
        if (!properties.isPersistSecurityEvents() || securityEventRepository == null) {
            return;
        }

        try {
            String eventId = UUID.randomUUID().toString();
            SecurityEvent event = new SecurityEvent(
                    eventId,
                    threatType != null ? threatType : ThreatType.UNKNOWN,
                    severity != null ? severity : ThreatSeverity.LOW,
                    score,
                    reason != null && reason.length() > 250 ? reason.substring(0, 250) : (reason != null ? reason : "Security event"),
                    action != null ? action : MitigationAction.ALLOWED,
                    context != null ? context.getPath() : null,
                    context != null ? context.getSourceIp() : "unknown"
            );
            if (evidence != null) {
                event.setEvidence(evidence);
            }
            securityEventRepository.save(event);
            log.debug("Persisted SecurityEvent {} for request {}", eventId, context != null ? context.getRequestId() : "N/A");
        } catch (Exception e) {
            log.error("Failed to persist SecurityEvent: {}", e.getMessage());
        }
    }

    private String formatDecisionReason(ThreatScoreResult scoreResult) {
        if (scoreResult == null || scoreResult.detectionResults() == null || scoreResult.detectionResults().isEmpty()) {
            return "No threats detected.";
        }

        List<String> reasons = scoreResult.detectionResults().stream()
                .filter(d -> d.detected() && d.reason() != null && !d.reason().isBlank())
                .map(d -> {
                    String r = d.reason().trim();
                    return r.endsWith(".") ? r.substring(0, r.length() - 1) : r;
                })
                .distinct()
                .toList();

        if (reasons.isEmpty()) {
            return "Suspicious traffic pattern detected.";
        }

        if (reasons.size() == 1) {
            return reasons.get(0) + ".";
        }

        // Combines multiple reasons: e.g. "High-risk API enumeration combined with abnormal request frequency."
        StringBuilder combined = new StringBuilder();
        for (int i = 0; i < reasons.size(); i++) {
            String r = reasons.get(i);
            if (i == 0) {
                combined.append(r);
            } else {
                combined.append(" combined with ")
                        .append(Character.toLowerCase(r.charAt(0)))
                        .append(r.substring(1));
            }
        }
        combined.append(".");
        return combined.toString();
    }

    private ThreatType resolvePrimaryThreatType(ThreatScoreResult scoreResult) {
        if (scoreResult == null || scoreResult.detectionResults() == null) {
            return ThreatType.UNKNOWN;
        }
        return scoreResult.detectionResults().stream()
                .filter(d -> d.detected() && d.threatType() != null)
                .map(com.apisentinel.detection.DetectionResult::threatType)
                .findFirst()
                .orElse(ThreatType.UNKNOWN);
    }

    private String extractEvidence(ThreatScoreResult scoreResult) {
        if (scoreResult == null || scoreResult.detectionResults() == null) {
            return null;
        }
        return scoreResult.detectionResults().stream()
                .filter(d -> d.detected() && d.evidence() != null && !d.evidence().isBlank())
                .map(com.apisentinel.detection.DetectionResult::evidence)
                .collect(Collectors.joining("; "));
    }

    private boolean hasThreatCategory(ThreatScoreResult scoreResult, ThreatCategory category) {
        if (scoreResult == null || scoreResult.detectionResults() == null) {
            return false;
        }
        return scoreResult.detectionResults().stream()
                .anyMatch(d -> d.detected() && d.getCategory() == category);
    }

    public PolicyEngineProperties getProperties() {
        return properties;
    }

    public void clearRateLimits() {
        rateLimitWindows.clear();
    }

    public void clearActiveBlockedSources() {
        activeBlockedSources.clear();
    }
}
