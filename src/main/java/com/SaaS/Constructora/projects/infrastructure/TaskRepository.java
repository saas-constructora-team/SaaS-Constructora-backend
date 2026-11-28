package com.SaaS.Constructora.projects.infrastructure;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<Task, UUID> {

    List<Task> findByTenantIdAndProjectIdOrderByCreatedAtDesc(UUID tenantId, UUID projectId);

    /** Lectura acotada para el tablero; el orden se define en el Pageable del servicio. */
    List<Task> findByTenantIdAndProjectIdIn(
            UUID tenantId, Collection<UUID> projectIds, Pageable pageable);

    /** Tarea por ID siempre dentro del tenant del usuario autenticado. */
    Optional<Task> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndProjectIdAndStatusId(
            UUID tenantId, UUID projectId, UUID statusId);
}
