package com.apisentinel.detection;

import com.apisentinel.gateway.ApiRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Modular Threat Detection Engine coordinating independent, deterministic detectors.
 * Executes each detector in isolation without dependencies between detection rules.
 */
@Service
public class ThreatDetectionEngine {

    private static final Logger log = LoggerFactory.getLogger(ThreatDetectionEngine.class);

    private final List<ThreatDetector> detectors;

    public ThreatDetectionEngine(List<ThreatDetector> detectors) {
        this.detectors = detectors != null ? detectors : Collections.emptyList();
        log.info("Initialized ThreatDetectionEngine with {} active detectors", this.detectors.size());
    }

    /**
     * Evaluates all registered detectors against the request context.
     *
     * @param context normalized request context
     * @return list of positive detection findings
     */
    public List<DetectionResult> evaluate(ApiRequestContext context) {
        List<DetectionResult> findings = new ArrayList<>();

        for (ThreatDetector detector : detectors) {
            try {
                DetectionResult result = detector.detect(context);
                if (result != null && result.detected()) {
                    log.warn(
                            "Threat Detected [{}] on {} {}: {}",
                            detector.getDetectorName(),
                            context.getHttpMethod(),
                            context.getPath(),
                            result.reason()
                    );
                    findings.add(result);
                }
            } catch (Exception e) {
                log.error("Error executing detector {}: {}", detector.getDetectorName(), e.getMessage(), e);
            }
        }

        return findings;
    }

    public List<ThreatDetector> getDetectors() {
        return Collections.unmodifiableList(detectors);
    }
}
