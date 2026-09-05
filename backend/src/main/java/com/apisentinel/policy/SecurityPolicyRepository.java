package com.apisentinel.policy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for SecurityPolicy entity.
 */
@Repository
public interface SecurityPolicyRepository extends JpaRepository<SecurityPolicy, Long> {

    Optional<SecurityPolicy> findByName(String name);

    List<SecurityPolicy> findByIsEnabledTrueOrderByPriorityAsc();

    List<SecurityPolicy> findByPolicyTypeAndIsEnabledTrue(PolicyType policyType);

    boolean existsByName(String name);
}
