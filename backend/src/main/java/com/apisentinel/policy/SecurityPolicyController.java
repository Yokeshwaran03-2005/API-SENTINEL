package com.apisentinel.policy;

import com.apisentinel.policy.dto.SecurityPolicyDto;
import com.apisentinel.policy.dto.UpdateSecurityPolicyRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller exposing security policies and threshold administration.
 */
@RestController
@RequestMapping("/api/policies")
public class SecurityPolicyController {

    private final SecurityPolicyService securityPolicyService;

    public SecurityPolicyController(SecurityPolicyService securityPolicyService) {
        this.securityPolicyService = securityPolicyService;
    }

    /**
     * GET /api/policies
     * Lists all security policies.
     */
    @GetMapping
    public ResponseEntity<List<SecurityPolicyDto>> getPolicies() {
        List<SecurityPolicyDto> policies = securityPolicyService.getAllPolicies();
        return ResponseEntity.ok(policies);
    }

    /**
     * GET /api/policies/{id}
     * Retrieves specific security policy by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SecurityPolicyDto> getPolicyById(@PathVariable Long id) {
        SecurityPolicyDto policy = securityPolicyService.getPolicyById(id);
        return ResponseEntity.ok(policy);
    }

    /**
     * PUT /api/policies/{id}
     * Updates an existing security policy with validation.
     */
    @PutMapping("/{id}")
    public ResponseEntity<SecurityPolicyDto> updatePolicy(
            @PathVariable Long id,
            @Valid @RequestBody UpdateSecurityPolicyRequest request
    ) {
        SecurityPolicyDto updated = securityPolicyService.updatePolicy(id, request);
        return ResponseEntity.ok(updated);
    }
}
