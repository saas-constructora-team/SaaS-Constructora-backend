package com.SaaS.Constructora.projects.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findByTenantIdAndProjectIdOrderByCreatedAtDesc(UUID tenantId, UUID projectId);

    boolean existsByTenantIdAndProjectIdAndStatusId(
            UUID tenantId, UUID projectId, UUID statusId);
}
