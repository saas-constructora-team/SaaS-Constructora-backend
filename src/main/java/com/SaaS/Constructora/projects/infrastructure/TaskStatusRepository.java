package com.SaaS.Constructora.projects.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskStatusRepository extends JpaRepository<TaskStatus, UUID> {

    List<TaskStatus> findByTenantIdAndProjectIdOrderByStatusOrderAsc(
            UUID tenantId, UUID projectId);

    List<TaskStatus> findByTenantIdAndProjectIdInOrderByStatusOrderAsc(
            UUID tenantId, Collection<UUID> projectIds);

    Optional<TaskStatus> findByIdAndTenantIdAndProjectId(
            UUID id, UUID tenantId, UUID projectId);

    Optional<TaskStatus> findByTenantIdAndProjectIdAndDefaultStatusTrue(
            UUID tenantId, UUID projectId);

    long countByTenantIdAndProjectId(UUID tenantId, UUID projectId);

    @Query("""
            select coalesce(max(s.statusOrder), 0)
            from TaskStatus s
            where s.tenantId = :tenantId and s.projectId = :projectId
            """)
    int findMaxStatusOrder(
            @Param("tenantId") UUID tenantId, @Param("projectId") UUID projectId);

    @Modifying
    @Query("""
            update TaskStatus s set s.defaultStatus = false
            where s.tenantId = :tenantId and s.projectId = :projectId
            """)
    void clearDefault(@Param("tenantId") UUID tenantId, @Param("projectId") UUID projectId);
}
