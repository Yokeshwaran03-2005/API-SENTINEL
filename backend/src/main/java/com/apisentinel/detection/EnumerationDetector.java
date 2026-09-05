package com.apisentinel.detection;

import com.apisentinel.events.ThreatSeverity;
import com.apisentinel.events.ThreatType;
import com.apisentinel.gateway.ApiRequestContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic detector for Object Enumeration, BOLA (Broken Object Level Authorization),
 * and IDOR attacks.
 * Identifies clients scanning sequential or iterating resource identifiers against parameterized routes.
 */
@Component
public class EnumerationDetector implements ThreatDetector {

    // Matches path segments that look like object identifiers (numeric IDs or UUIDs)
    private static final Pattern ID_SEGMENT_PATTERN = Pattern.compile(
            "^(.*?)/(\\d+|[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})(/.*)?$"
    );

    private final int distinctIdThreshold;
    private final int windowSeconds;
    private final double scoreContribution;

    // Key: sourceIp + "|" + pathTemplate -> List of Record entries (id, timestamp)
    private final Map<String, List<IdAccessRecord>> accessTracker = new ConcurrentHashMap<>();

    public EnumerationDetector(
            @Value("${sentinel.detection.enumeration.threshold:5}") int distinctIdThreshold,
            @Value("${sentinel.detection.enumeration.window-seconds:60}") int windowSeconds,
            @Value("${sentinel.detection.enumeration.score:80.0}") double scoreContribution
    ) {
        this.distinctIdThreshold = distinctIdThreshold;
        this.windowSeconds = windowSeconds;
        this.scoreContribution = scoreContribution;
    }

    @Override
    public String getDetectorName() {
        return "EnumerationDetector";
    }

    @Override
    public DetectionResult detect(ApiRequestContext context) {
        String path = context.getPath();
        if (path == null) {
            return DetectionResult.clean();
        }

        Matcher matcher = ID_SEGMENT_PATTERN.matcher(path);
        if (!matcher.matches()) {
            return DetectionResult.clean();
        }

        String prefix = matcher.group(1);
        String targetId = matcher.group(2);
        String suffix = matcher.group(3) != null ? matcher.group(3) : "";
        String pathTemplate = prefix + "/{id}" + suffix;

        String sourceKey = context.getSourceIp() + "|" + pathTemplate;
        Instant now = context.getTimestamp();
        Instant cutoff = now.minusSeconds(windowSeconds);

        List<IdAccessRecord> records = accessTracker.computeIfAbsent(
                sourceKey,
                k -> Collections.synchronizedList(new ArrayList<>())
        );

        synchronized (records) {
            // Evict expired entries
            records.removeIf(record -> record.timestamp().isBefore(cutoff));

            // Record current access
            records.add(new IdAccessRecord(targetId, now));

            // Count unique identifiers accessed in the window
            Set<String> uniqueIds = new HashSet<>();
            for (IdAccessRecord record : records) {
                uniqueIds.add(record.id());
            }

            if (uniqueIds.size() > distinctIdThreshold) {
                return DetectionResult.detected(
                        ThreatType.BOLA_IDOR,
                        ThreatSeverity.HIGH,
                        scoreContribution,
                        String.format("Resource enumeration probe detected on %s (%d distinct IDs in %ds)",
                                pathTemplate, uniqueIds.size(), windowSeconds),
                        "Enumerated identifiers: " + uniqueIds
                );
            }
        }

        return DetectionResult.clean();
    }

    public void resetTracker() {
        accessTracker.clear();
    }

    private record IdAccessRecord(String id, Instant timestamp) {
    }
}
