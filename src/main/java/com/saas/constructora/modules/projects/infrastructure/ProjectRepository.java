package com.saas.constructora.modules.projects.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    List<Project> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);

    List<Project> findByTenantIdAndIdInOrderByCreatedAtDesc(UUID tenantId, Collection<UUID> ids);

    Optional<Project> findByIdAndTenantId(UUID id, UUID tenantId);
}