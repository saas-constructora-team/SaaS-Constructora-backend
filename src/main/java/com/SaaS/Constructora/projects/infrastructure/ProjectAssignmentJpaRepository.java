package com.SaaS.Constructora.projects.infrastructure;

import com.SaaS.Constructora.projects.domain.ProjectAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ProjectAssignmentJpaRepository extends JpaRepository<ProjectAssignment, UUID> {

    List<ProjectAssignment> findByTenantIdAndProjectIdAndUnassignedAtIsNullOrderByAssignedAtAsc(
            UUID tenantId, UUID projectId);

    List<ProjectAssignment> findByTenantIdAndUnassignedAtIsNull(UUID tenantId);

    List<ProjectAssignment> findByTenantIdAndUserIdAndUnassignedAtIsNull(UUID tenantId, UUID userId);

    Optional<ProjectAssignment> findByTenantIdAndProjectIdAndUserIdAndUnassignedAtIsNull(
            UUID tenantId, UUID projectId, UUID userId);

    boolean existsByTenantIdAndProjectIdAndUserIdAndUnassignedAtIsNull(
            UUID tenantId, UUID projectId, UUID userId);

    @Modifying
    @Query("""
            update ProjectAssignment a set a.unassignedAt = :at, a.unassignedBy = :by
            where a.tenantId = :tenantId and a.projectId = :projectId
              and a.userId = :userId and a.unassignedAt is null
            """)
    int unassign(@Param("tenantId") UUID tenantId, @Param("projectId") UUID projectId,
                 @Param("userId") UUID userId, @Param("by") UUID by,
                 @Param("at") LocalDateTime at);

    @Modifying
    @Query("""
            update ProjectAssignment a set a.unassignedAt = :at, a.unassignedBy = :by
            where a.tenantId = :tenantId and a.userId = :userId and a.unassignedAt is null
            """)
    int unassignAllForUser(@Param("tenantId") UUID tenantId, @Param("userId") UUID userId,
                           @Param("by") UUID by, @Param("at") LocalDateTime at);

    @Query("""
            select a.projectId from ProjectAssignment a
            where a.tenantId = :tenantId and a.userId = :userId and a.unassignedAt is null
            """)
    List<UUID> findProjectIdsByTenantIdAndUserId(
            @Param("tenantId") UUID tenantId, @Param("userId") UUID userId);
}
