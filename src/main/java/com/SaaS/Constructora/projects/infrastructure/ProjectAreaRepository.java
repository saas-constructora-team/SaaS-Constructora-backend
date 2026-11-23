package com.SaaS.Constructora.projects.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectAreaRepository extends JpaRepository<ProjectArea, UUID> {

    List<ProjectArea> findByTenantIdAndProjectIdOrderByNameAsc(UUID tenantId, UUID projectId);

    Optional<ProjectArea> findByIdAndTenantIdAndProjectId(
            UUID id, UUID tenantId, UUID projectId);
}
