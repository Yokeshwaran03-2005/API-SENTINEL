package com.apisentinel.gateway;

import com.apisentinel.detection.DetectionResult;
import com.apisentinel.detection.ThreatDetectionEngine;
import com.apisentinel.policy.PolicyEngine;
import com.apisentinel.policy.SecurityDecision;
import com.apisentinel.scoring.ThreatScoreResult;
import com.apisentinel.scoring.ThreatScoringEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Default implementation of SecurityPipeline.
 * Coordinates Request Interception (Phase 4), Threat Detection (Phase 5),
 * Threat Scoring (Phase 6), and Policy Decision Enforcement (Phase 7),
 * and persists request telemetry to ApiRequestRepository (Phase 8).
 */
@Component
public class DefaultSecurityPipeline implements SecurityPipeline {

    private static final Logger log = LoggerFactory.getLogger(DefaultSecurityPipeline.class);

    private final ThreatDetectionEngine detectionEngine;
    private final ThreatScoringEngine scoringEngine;
    private final PolicyEngine policyEngine;
    private final ApiRequestRepository apiRequestRepository;

    @Autowired
    public DefaultSecurityPipeline(
            ThreatDetectionEngine detectionEngine,
            ThreatScoringEngine scoringEngine,
            PolicyEngine policyEngine,
            ApiRequestRepository apiRequestRepository
    ) {
        this.detectionEngine = detectionEngine;
        this.scoringEngine = scoringEngine;
        this.policyEngine = policyEngine;
        this.apiRequestRepository = apiRequestRepository;
    }

    public DefaultSecurityPipeline(
            ThreatDetectionEngine detectionEngine,
            ThreatScoringEngine scoringEngine,
            PolicyEngine policyEngine
    ) {
        this(detectionEngine, scoringEngine, policyEngine, null);
    }

    @Override
    public InspectionResult inspect(ApiRequestContext context) {
        log.debug(
                "Inspecting request: id={}, method={}, path={}, ip={}, auth={}, size={}",
                context.getRequestId(),
                context.getHttpMethod(),
                context.getPath(),
                context.getSourceIp(),
                context.getAuthStatus(),
                context.getRequestSizeBytes()
        );

        // 1. Execute modular Threat Detection Engine (Phase 5)
        List<DetectionResult> detections = detectionEngine.evaluate(context);

        // 2. Execute Threat Scoring Engine (Phase 6)
        ThreatScoreResult scoreResult = scoringEngine.score(detections);

        // 3. Execute Policy Engine to produce authoritative SecurityDecision (Phase 7)
        SecurityDecision decision = policyEngine.evaluate(context, scoreResult);

        log.info(
                "Request {} Decision: {} (Score: {} [{}], Reason: {})",
                context.getRequestId(),
                decision.action(),
                decision.threatScore(),
                decision.riskLevel(),
                decision.reason()
        );

        InspectionResult result = InspectionResult.fromDecision(decision);

        // 4. Persist telemetry to ApiRequest repository for dashboard visibility (Phase 8)
        if (apiRequestRepository != null) {
            try {
                ApiRequest entity = new ApiRequest(
                        context.getRequestId(),
                        context.getHttpMethod(),
                        context.getPath(),
                        context.getSourceIp()
                );
                entity.setUserAgent(context.getUserAgent());
                entity.setClientIdentifier(context.getClientIdentifier());
                entity.setRequestSizeBytes(context.getRequestSizeBytes());
                entity.setAuthStatus(context.getAuthStatus());
                entity.setVerdict(result.verdict());
                entity.setThreatScore((double) decision.threatScore());
                apiRequestRepository.save(entity);
            } catch (Exception e) {
                log.error("Failed to persist ApiRequest: {}", e.getMessage());
            }
        }

        return result;
    }

    public ThreatScoringEngine getScoringEngine() {
        return scoringEngine;
    }

    public ThreatDetectionEngine getDetectionEngine() {
        return detectionEngine;
    }

    public PolicyEngine getPolicyEngine() {
        return policyEngine;
    }

    public ApiRequestRepository getApiRequestRepository() {
        return apiRequestRepository;
    }
}
