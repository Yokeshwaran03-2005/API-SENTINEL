package com.apisentinel.policy.dto;

import com.apisentinel.policy.PolicyAction;
import com.apisentinel.policy.PolicyType;
import com.apisentinel.policy.SecurityPolicy;

import java.time.Instant;

/**
 * Data transfer object for SecurityPolicy.
 */
public record SecurityPolicyDto(
        Long id,
        String name,
        String description,
        PolicyType policyType,
        PolicyAction actionOnBreach,
        String endpointPattern,
        Integer requestThreshold,
        Integer timeWindowSeconds,
        Double threatScoreThreshold,
        boolean isEnabled,
        int priority,
        Instant createdAt,
        Instant updatedAt
) {
    public static SecurityPolicyDto fromEntity(SecurityPolicy policy) {
        if (policy == null) return null;
        return new SecurityPolicyDto(
                policy.getId(),
                policy.getName(),
                policy.getDescription(),
                policy.getPolicyType(),
                policy.getActionOnBreach(),
                policy.getEndpointPattern(),
                policy.getRequestThreshold(),
                policy.getTimeWindowSeconds(),
                policy.getThreatScoreThreshold(),
                policy.isEnabled(),
                policy.getPriority(),
                policy.getCreatedAt(),
                policy.getUpdatedAt()
        );
    }
}
