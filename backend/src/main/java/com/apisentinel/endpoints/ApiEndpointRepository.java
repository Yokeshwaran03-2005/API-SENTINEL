package com.apisentinel.endpoints;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for ApiEndpoint entity.
 */
@Repository
public interface ApiEndpointRepository extends JpaRepository<ApiEndpoint, Long> {

    Optional<ApiEndpoint> findByPathAndHttpMethod(String path, HttpMethod httpMethod);

    List<ApiEndpoint> findByIsMonitoredTrue();

    List<ApiEndpoint> findBySensitivityLevel(SensitivityLevel sensitivityLevel);

    boolean existsByPathAndHttpMethod(String path, HttpMethod httpMethod);
}
