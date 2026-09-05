package com.apisentinel.policy.dto;

import com.apisentinel.policy.PolicyAction;
import com.apisentinel.policy.PolicyType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Validated request body for updating a SecurityPolicy.
 */
public record UpdateSecurityPolicyRequest(
        @Size(max = 100, message = "Policy name must not exceed 100 characters")
        String name,

        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description,

        PolicyType policyType,

        PolicyAction actionOnBreach,

        @Size(max = 255, message = "Endpoint pattern must not exceed 255 characters")
        String endpointPattern,

        @PositiveOrZero(message = "Request threshold must be non-negative")
        Integer requestThreshold,

        @PositiveOrZero(message = "Time window must be non-negative")
        Integer timeWindowSeconds,

        @Min(value = 0, message = "Threat score threshold must be at least 0")
        @Max(value = 100, message = "Threat score threshold must not exceed 100")
        Double threatScoreThreshold,

        Boolean isEnabled,

        Integer priority
) {}
