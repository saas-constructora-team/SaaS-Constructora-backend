package com.SaaS.Constructora.projects.infrastructure;

import com.SaaS.Constructora.projects.application.ports.ProjectAssignmentRepository;
import com.SaaS.Constructora.projects.domain.ProjectAssignment;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador JPA del puerto de asignaciones: la persistencia no sale de projects. */
@Repository
class ProjectAssignmentRepositoryAdapter implements ProjectAssignmentRepository {

    private final ProjectAssignmentJpaRepository repository;

    ProjectAssignmentRepositoryAdapter(ProjectAssignmentJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ProjectAssignment> findActiveByTenantIdAndProjectIdOrderByAssignedAtAsc(
            UUID tenantId, UUID projectId) {
        return repository.findByTenantIdAndProjectIdAndUnassignedAtIsNullOrderByAssignedAtAsc(
                tenantId, projectId);
    }

    @Override
    public List<ProjectAssignment> findActiveByTenantId(UUID tenantId) {
        return repository.findByTenantIdAndUnassignedAtIsNull(tenantId);
    }

    @Override
    public List<ProjectAssignment> findActiveByTenantIdAndProjectIdIn(
            UUID tenantId, Collection<UUID> projectIds) {
        return repository.findByTenantIdAndProjectIdInAndUnassignedAtIsNull(tenantId, projectIds);
    }

    @Override
    public List<ProjectAssignment> findActiveByTenantIdAndUserId(UUID tenantId, UUID userId) {
        return repository.findByTenantIdAndUserIdAndUnassignedAtIsNull(tenantId, userId);
    }

    @Override
    public Optional<ProjectAssignment> findActiveByTenantIdAndProjectIdAndUserId(
            UUID tenantId, UUID projectId, UUID userId) {
        return repository.findByTenantIdAndProjectIdAndUserIdAndUnassignedAtIsNull(
                tenantId, projectId, userId);
    }

    @Override
    public Optional<ProjectAssignment> findActiveByIdAndTenantIdAndProjectId(
            UUID id, UUID tenantId, UUID projectId) {
        return repository.findByIdAndTenantIdAndProjectIdAndUnassignedAtIsNull(
                id, tenantId, projectId);
    }

    @Override
    public boolean existsActiveByTenantIdAndProjectIdAndUserId(
            UUID tenantId, UUID projectId, UUID userId) {
        return repository.existsByTenantIdAndProjectIdAndUserIdAndUnassignedAtIsNull(
                tenantId, projectId, userId);
    }

    @Override
    public ProjectAssignment save(ProjectAssignment assignment) {
        return repository.save(assignment);
    }

    @Override
    public int unassign(UUID tenantId, UUID projectId, UUID userId,
                        UUID unassignedBy, LocalDateTime unassignedAt) {
        return repository.unassign(tenantId, projectId, userId, unassignedBy, unassignedAt);
    }

    @Override
    public int unassignAllForUser(UUID tenantId, UUID userId,
                                  UUID unassignedBy, LocalDateTime unassignedAt) {
        return repository.unassignAllForUser(tenantId, userId, unassignedBy, unassignedAt);
    }

    @Override
    public List<UUID> findProjectIdsByTenantIdAndUserId(UUID tenantId, UUID userId) {
        return repository.findProjectIdsByTenantIdAndUserId(tenantId, userId);
    }
}
