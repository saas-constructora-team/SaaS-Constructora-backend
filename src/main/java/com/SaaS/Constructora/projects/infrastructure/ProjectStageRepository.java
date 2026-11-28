package com.SaaS.Constructora.projects.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectStageRepository extends JpaRepository<ProjectStage, UUID> {

    List<ProjectStage> findByTenantIdAndProjectIdOrderByStageOrderAsc(
            UUID tenantId, UUID projectId);

    List<ProjectStage> findByTenantIdAndProjectIdInOrderByStageOrderAsc(
            UUID tenantId, Collection<UUID> projectIds);

    Optional<ProjectStage> findByIdAndTenantIdAndProjectId(
            UUID id, UUID tenantId, UUID projectId);

    @Query("""
            select coalesce(max(s.stageOrder), 0)
            from ProjectStage s
            where s.tenantId = :tenantId and s.projectId = :projectId
            """)
    int findMaxStageOrder(
            @Param("tenantId") UUID tenantId, @Param("projectId") UUID projectId);
}
