package com.SaaS.Constructora.projects.application.ports;

import com.SaaS.Constructora.projects.domain.ProjectAssignment;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Contrato del módulo projects para persistir asignaciones activas. */
public interface ProjectAssignmentRepository {

    List<ProjectAssignment> findActiveByTenantIdAndProjectIdOrderByAssignedAtAsc(
            UUID tenantId, UUID projectId);

    List<ProjectAssignment> findActiveByTenantId(UUID tenantId);

    List<ProjectAssignment> findActiveByTenantIdAndProjectIdIn(
            UUID tenantId, Collection<UUID> projectIds);

    List<ProjectAssignment> findActiveByTenantIdAndUserId(UUID tenantId, UUID userId);

    Optional<ProjectAssignment> findActiveByTenantIdAndProjectIdAndUserId(
            UUID tenantId, UUID projectId, UUID userId);

    Optional<ProjectAssignment> findActiveByIdAndTenantIdAndProjectId(
            UUID id, UUID tenantId, UUID projectId);

    boolean existsActiveByTenantIdAndProjectIdAndUserId(
            UUID tenantId, UUID projectId, UUID userId);

    ProjectAssignment save(ProjectAssignment assignment);

    /** Borrado lógico de una asignación; devuelve el número de filas afectadas. */
    int unassign(UUID tenantId, UUID projectId, UUID userId,
                 UUID unassignedBy, LocalDateTime unassignedAt);

    /** Borrado lógico de todas las asignaciones de un usuario en el tenant. */
    int unassignAllForUser(UUID tenantId, UUID userId,
                           UUID unassignedBy, LocalDateTime unassignedAt);

    /** IDs de proyectos asignados a un usuario dentro de su empresa. */
    List<UUID> findProjectIdsByTenantIdAndUserId(UUID tenantId, UUID userId);
}
