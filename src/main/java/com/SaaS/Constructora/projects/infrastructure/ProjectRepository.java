package com.SaaS.Constructora.projects.infrastructure;
import com.SaaS.Constructora.projects.domain.ProjectStatus;
import com.SaaS.Constructora.projects.domain.ProjectType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    /**
     * Lista de proyectos visible para el usuario en una sola consulta:
     * ADMIN ve todos los del tenant; el resto solo los asignados.
     */
    @Query("""
            select p from Project p
            where p.tenantId = :tenantId
              and (:admin = true or exists (
                  select 1 from ProjectAssignment a
                  where a.tenantId = :tenantId
                    and a.userId = :userId
                    and a.projectId = p.id
                    and a.unassignedAt is null))
            order by p.createdAt desc, p.id desc
            """)
    List<Project> findVisibleProjects(
            @Param("tenantId") UUID tenantId,
            @Param("userId") UUID userId,
            @Param("admin") boolean admin);

    /** Búsqueda paginada y filtrada, siempre restringida al tenant y a la visibilidad del usuario. */
    @Query(value = """
            select p from Project p
            where p.tenantId = :tenantId
              and (:admin = true or exists (
                  select 1 from ProjectAssignment a
                  where a.tenantId = :tenantId
                    and a.userId = :userId
                    and a.projectId = p.id
                    and a.unassignedAt is null))
              and (:namePattern is null or lower(p.name) like :namePattern
                                       or lower(p.clientName) like :namePattern)
              and (:status is null or p.status = :status)
              and (:projectType is null or p.projectType = :projectType)
              and p.createdAt >= coalesce(:from, p.createdAt)
              and p.createdAt <= coalesce(:to, p.createdAt)
            """,
            countQuery = """
            select count(p) from Project p
            where p.tenantId = :tenantId
              and (:admin = true or exists (
                  select 1 from ProjectAssignment a
                  where a.tenantId = :tenantId
                    and a.userId = :userId
                    and a.projectId = p.id
                    and a.unassignedAt is null))
              and (:namePattern is null or lower(p.name) like :namePattern
                                       or lower(p.clientName) like :namePattern)
              and (:status is null or p.status = :status)
              and (:projectType is null or p.projectType = :projectType)
              and p.createdAt >= coalesce(:from, p.createdAt)
              and p.createdAt <= coalesce(:to, p.createdAt)
            """)
    Page<Project> searchProjects(
            @Param("tenantId") UUID tenantId,
            @Param("userId") UUID userId,
            @Param("admin") boolean admin,
            @Param("namePattern") String namePattern,
            @Param("status") ProjectStatus status,
            @Param("projectType") ProjectType projectType,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    Optional<Project> findByIdAndTenantId(UUID id, UUID tenantId);
}
