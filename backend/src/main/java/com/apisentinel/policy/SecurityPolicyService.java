package com.apisentinel.policy;

import com.apisentinel.common.exception.ResourceNotFoundException;
import com.apisentinel.policy.dto.SecurityPolicyDto;
import com.apisentinel.policy.dto.UpdateSecurityPolicyRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service managing security policies, threshold configurations, and rule enforcement states.
 */
@Service
@Transactional
public class SecurityPolicyService {

    private final SecurityPolicyRepository securityPolicyRepository;

    public SecurityPolicyService(SecurityPolicyRepository securityPolicyRepository) {
        this.securityPolicyRepository = securityPolicyRepository;
    }

    /**
     * Lists all security policies ordered by priority.
     */
    @Transactional(readOnly = true)
    public List<SecurityPolicyDto> getAllPolicies() {
        return securityPolicyRepository.findAll().stream()
                .map(SecurityPolicyDto::fromEntity)
                .toList();
    }

    /**
     * Retrieves a security policy by ID.
     */
    @Transactional(readOnly = true)
    public SecurityPolicyDto getPolicyById(Long id) {
        SecurityPolicy policy = securityPolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SecurityPolicy", id));
        return SecurityPolicyDto.fromEntity(policy);
    }

    /**
     * Updates an existing security policy with validated fields.
     */
    public SecurityPolicyDto updatePolicy(Long id, UpdateSecurityPolicyRequest request) {
        SecurityPolicy policy = securityPolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SecurityPolicy", id));

        if (request.name() != null && !request.name().isBlank()) {
            policy.setName(request.name().trim());
        }
        if (request.description() != null) {
            policy.setDescription(request.description().trim());
        }
        if (request.policyType() != null) {
            policy.setPolicyType(request.policyType());
        }
        if (request.actionOnBreach() != null) {
            policy.setActionOnBreach(request.actionOnBreach());
        }
        if (request.endpointPattern() != null && !request.endpointPattern().isBlank()) {
            policy.setEndpointPattern(request.endpointPattern().trim());
        }
        if (request.requestThreshold() != null) {
            policy.setRequestThreshold(request.requestThreshold());
        }
        if (request.timeWindowSeconds() != null) {
            policy.setTimeWindowSeconds(request.timeWindowSeconds());
        }
        if (request.threatScoreThreshold() != null) {
            policy.setThreatScoreThreshold(request.threatScoreThreshold());
        }
        if (request.isEnabled() != null) {
            policy.setEnabled(request.isEnabled());
        }
        if (request.priority() != null) {
            policy.setPriority(request.priority());
        }

        SecurityPolicy updated = securityPolicyRepository.save(policy);
        return SecurityPolicyDto.fromEntity(updated);
    }
}
