package com.apisentinel.policy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for BlockedSource entity.
 */
@Repository
public interface BlockedSourceRepository extends JpaRepository<BlockedSource, Long> {

    Optional<BlockedSource> findBySourceValueAndIsActiveTrue(String sourceValue);

    List<BlockedSource> findByIsActiveTrue();

    boolean existsBySourceValueAndIsActiveTrue(String sourceValue);

    List<BlockedSource> findBySourceTypeAndIsActiveTrue(SourceType sourceType);
}
