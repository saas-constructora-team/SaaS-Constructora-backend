package com.SaaS.Constructora.sitelog.application;

import com.SaaS.Constructora.documents.application.ports.DocumentTenantPort;
import com.SaaS.Constructora.identity.domain.Role;
import com.SaaS.Constructora.projects.application.ports.ProjectTenantPort;
import com.SaaS.Constructora.shared.events.ProjectChanged;
import com.SaaS.Constructora.sitelog.application.ports.SiteLogRepository;
import com.SaaS.Constructora.sitelog.domain.SiteLogEntry;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Asocia documentos a etapas de proyecto. Toda validación de pertenencia se
 * delega a los contratos públicos de projects y documents: site-log no consulta
 * sus tablas ni conoce su persistencia.
 */
@Service
public class SiteLogService {

    private final SiteLogRepository repository;
    private final ProjectTenantPort projects;
    private final DocumentTenantPort documents;
    private final ApplicationEventPublisher events;

    public SiteLogService(SiteLogRepository repository, ProjectTenantPort projects,
                          DocumentTenantPort documents, ApplicationEventPublisher events) {
        this.repository = repository;
        this.projects = projects;
        this.documents = documents;
        this.events = events;
    }

    @Transactional
    public SiteLogEntry associate(UUID tenantId, UUID projectId, UUID stageId,
                                  UUID userId, Role role, UUID documentId) {
        requireAccess(tenantId, projectId, userId, role);
        projects.ensureStageBelongsToProjectAndTenant(stageId, projectId, tenantId);
        projects.ensureStageAllowsEntries(stageId);
        if (documentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El documento es obligatorio");
        }
        if (!documents.existsInTenant(documentId, tenantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento no encontrado");
        }
        if (repository.existsByDocumentIdAndTenantId(documentId, tenantId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El documento ya está asociado a una etapa");
        }
        SiteLogEntry saved = repository.save(new SiteLogEntry(UUID.randomUUID(), tenantId,
                projectId, stageId, documentId, userId, LocalDateTime.now()));
        events.publishEvent(ProjectChanged.of(tenantId, projectId,
                ProjectChanged.SITE_LOG, saved.id(), userId));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<SiteLogEntry> list(UUID tenantId, UUID projectId, UUID stageId,
                                   UUID userId, Role role) {
        requireAccess(tenantId, projectId, userId, role);
        projects.ensureStageBelongsToProjectAndTenant(stageId, projectId, tenantId);
        return repository.findByTenantIdAndProjectIdAndStageId(tenantId, projectId, stageId);
    }

    /** Proyecto inexistente o no asignado se reporta como 404 para no revelarlo. */
    private void requireAccess(UUID tenantId, UUID projectId, UUID userId, Role role) {
        projects.ensureBelongsToTenant(projectId, tenantId);
        if (!role.canViewAllProjects()
                && !projects.isUserAssignedToProject(projectId, tenantId, userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado");
        }
    }
}
