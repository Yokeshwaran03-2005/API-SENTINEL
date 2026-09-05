package com.apisentinel.detection;

import com.apisentinel.events.ThreatSeverity;
import com.apisentinel.events.ThreatType;
import com.apisentinel.gateway.ApiRequestContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Deterministic rate abuse detector using an in-memory sliding window algorithm.
 * Evaluates both sustained volume surges and rapid burst floods per source IP.
 */
@Component
public class RateAbuseDetector implements ThreatDetector {

    private final int maxRequests;
    private final int windowSeconds;
    private final int burstThreshold;
    private final int burstSeconds;
    private final double scoreContribution;

    private final Map<String, Deque<Instant>> requestLog = new ConcurrentHashMap<>();

    public RateAbuseDetector(
            @Value("${sentinel.detection.rate.max-requests:60}") int maxRequests,
            @Value("${sentinel.detection.rate.window-seconds:60}") int windowSeconds,
            @Value("${sentinel.detection.rate.burst-threshold:15}") int burstThreshold,
            @Value("${sentinel.detection.rate.burst-seconds:3}") int burstSeconds,
            @Value("${sentinel.detection.rate.score:65.0}") double scoreContribution
    ) {
        this.maxRequests = maxRequests;
        this.windowSeconds = windowSeconds;
        this.burstThreshold = burstThreshold;
        this.burstSeconds = burstSeconds;
        this.scoreContribution = scoreContribution;
    }

    @Override
    public String getDetectorName() {
        return "RateAbuseDetector";
    }

    @Override
    public DetectionResult detect(ApiRequestContext context) {
        String sourceKey = context.getSourceIp();
        if (sourceKey == null || sourceKey.isBlank()) {
            return DetectionResult.clean();
        }

        Instant now = context.getTimestamp();
        Instant windowCutoff = now.minusSeconds(windowSeconds);
        Instant burstCutoff = now.minusSeconds(burstSeconds);

        Deque<Instant> timestamps = requestLog.computeIfAbsent(sourceKey, k -> new ArrayDeque<>());

        synchronized (timestamps) {
            // Evict requests outside sliding window
            while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(windowCutoff)) {
                timestamps.pollFirst();
            }

            timestamps.addLast(now);

            // 1. Check for immediate burst attack (e.g. >15 requests in 3s)
            long burstCount = timestamps.stream()
                    .filter(ts -> !ts.isBefore(burstCutoff))
                    .count();

            if (burstCount > burstThreshold) {
                return DetectionResult.detected(
                        ThreatType.ANOMALOUS_BURST,
                        ThreatSeverity.HIGH,
                        scoreContribution + 15.0,
                        String.format("Volumetric burst detected: %d requests in %ds from IP %s",
                                burstCount, burstSeconds, sourceKey),
                        String.format("Burst rate threshold: %d req/%ds", burstThreshold, burstSeconds)
                );
            }

            // 2. Check for sustained sliding window rate abuse (e.g. >60 requests in 60s)
            if (timestamps.size() > maxRequests) {
                return DetectionResult.detected(
                        ThreatType.RATE_LIMIT_EXCEEDED,
                        ThreatSeverity.MEDIUM,
                        scoreContribution,
                        String.format("Exceeded sustained rate quota: %d requests in %ds (limit: %d)",
                                timestamps.size(), windowSeconds, maxRequests),
                        String.format("Source %s sent %d requests within %ds window",
                                sourceKey, timestamps.size(), windowSeconds)
                );
            }
        }

        return DetectionResult.clean();
    }

    public void resetTracker() {
        requestLog.clear();
    }
}
