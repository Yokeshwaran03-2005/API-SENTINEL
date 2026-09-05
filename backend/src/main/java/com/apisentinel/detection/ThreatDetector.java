package com.apisentinel.detection;

import com.apisentinel.gateway.ApiRequestContext;

/**
 * Common contract for modular, independent, and deterministic threat detectors.
 * Detectors do not rely on non-deterministic external AI/ML services.
 */
public interface ThreatDetector {

    /**
     * Unique detector identifier.
     */
    String getDetectorName();

    /**
     * Evaluates incoming request context and returns an explainable detection result.
     *
     * @param context normalized security request context
     * @return DetectionResult indicating whether a threat was detected and corresponding evidence
     */
    DetectionResult detect(ApiRequestContext context);
}
