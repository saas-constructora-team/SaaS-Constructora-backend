package com.saas.constructora.modules.projects.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProjectAssignmentRepository extends JpaRepository<ProjectAssignment, UUID> {

    List<ProjectAssignment> findByTenantIdAndProjectIdOrderByAssignedAtAsc(
            UUID tenantId, UUID projectId);

    List<ProjectAssignment> findByTenantId(UUID tenantId);

    boolean existsByTenantIdAndProjectIdAndUserId(UUID tenantId, UUID projectId, UUID userId);

    long deleteByTenantIdAndProjectIdAndUserId(UUID tenantId, UUID projectId, UUID userId);

    /** IDs de proyectos asignados a un usuario dentro de su empresa. */
    @Query("""
            select a.projectId
            from ProjectAssignment a
            where a.tenantId = :tenantId and a.userId = :userId
            """)
    List<UUID> findProjectIdsByTenantIdAndUserId(
            @Param("tenantId") UUID tenantId, @Param("userId") UUID userId);
}
