package com.SaaS.Constructora.personnel;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface WorkerPositionRepository extends JpaRepository<WorkerPosition, UUID> {

    List<WorkerPosition> findByTenantIdOrderByNameAsc(UUID tenantId);

    Optional<WorkerPosition> findByTenantIdAndId(UUID tenantId, UUID id);

    Optional<WorkerPosition> findByTenantIdAndName(UUID tenantId, String name);

    boolean existsByTenantIdAndName(UUID tenantId, String name);

    boolean existsByTenantIdAndNameAndIdNot(UUID tenantId, String name, UUID id);
}
