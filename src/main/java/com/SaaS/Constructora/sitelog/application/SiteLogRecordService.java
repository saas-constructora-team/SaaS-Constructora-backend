package com.SaaS.Constructora.sitelog.application;

import com.SaaS.Constructora.identity.application.SiteLogManagementPermissionService;
import com.SaaS.Constructora.identity.application.UserSummary;
import com.SaaS.Constructora.identity.application.ports.UserQueryPort;
import com.SaaS.Constructora.projects.application.ports.ProjectTenantPort;
import com.SaaS.Constructora.shared.events.ProjectChanged;
import com.SaaS.Constructora.shared.security.AuthenticatedUser;
import com.SaaS.Constructora.sitelog.application.ports.SiteLogRecordRepository;
import com.SaaS.Constructora.sitelog.domain.SiteLogRecord;
import com.SaaS.Constructora.sitelog.domain.SiteLogType;
import com.SaaS.Constructora.sitelog.interfaces.SiteLogRecordRequest;
import com.SaaS.Constructora.sitelog.interfaces.SiteLogRecordResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Casos de uso de la bitácora de campo: alta, consulta, edición y borrado. */
@Service
public class SiteLogRecordService {

    private final SiteLogRecordRepository repository;
    private final ProjectTenantPort projects;
    private final UserQueryPort users;
    private final SiteLogManagementPermissionService siteLogPermissions;
    private final ApplicationEventPublisher events;

    public SiteLogRecordService(SiteLogRecordRepository repository, ProjectTenantPort projects,
                                UserQueryPort users,
                                SiteLogManagementPermissionService siteLogPermissions,
                                ApplicationEventPublisher events) {
        this.repository = repository;
        this.projects = projects;
        this.users = users;
        this.siteLogPermissions = siteLogPermissions;
        this.events = events;
    }

    @Transactional
    public SiteLogRecordResponse create(UUID tenantId, UUID projectId, AuthenticatedUser user,
                                        SiteLogRecordRequest request) {
        requireProjectAccess(tenantId, projectId, user);
        UserSummary author = users.ensureBelongsToTenant(user.userId(), tenantId);
        UserSummary assignee = requireAssignee(tenantId, request.assigneeId());
        LocalDateTime now = LocalDateTime.now();

        SiteLogRecord record = new SiteLogRecord(
                UUID.randomUUID(), tenantId, projectId, request.type(), request.status(),
                trim(request.what()), trim(request.how()), request.when(), trim(request.where()),
                trimToNull(request.why()), author.id(),
                assignee == null ? null : assignee.id(), now, now);

        SiteLogRecord saved = repository.save(record);
        notifyChange(tenantId, projectId, user.userId(), saved.id());
        return toResponse(saved, author, assignee);
    }

    @Transactional(readOnly = true)
    public List<SiteLogRecordResponse> list(UUID tenantId, UUID projectId, AuthenticatedUser user,
                                            SiteLogType type, String query,
                                            LocalDateTime from, LocalDateTime to) {
        requireProjectAccess(tenantId, projectId, user);
        return repository.findByTenantIdAndProjectId(tenantId, projectId, type, query, from, to)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SiteLogRecordResponse update(UUID tenantId, UUID projectId, UUID entryId,
                                        AuthenticatedUser user, SiteLogRecordRequest request) {
        requireProjectAccess(tenantId, projectId, user);
        SiteLogRecord current = requireRecord(tenantId, projectId, entryId);
        requireEditPermission(tenantId, current, user);
        UserSummary assignee = requireAssignee(tenantId, request.assigneeId());

        SiteLogRecord updated = new SiteLogRecord(
                current.id(), current.tenantId(), current.projectId(),
                request.type(), request.status(), trim(request.what()), trim(request.how()),
                request.when(), trim(request.where()), trimToNull(request.why()),
                current.authorId(), assignee == null ? null : assignee.id(),
                current.createdAt(), LocalDateTime.now());

        UserSummary author = users.ensureBelongsToTenant(current.authorId(), tenantId);
        SiteLogRecord saved = repository.save(updated);
        notifyChange(tenantId, projectId, user.userId(), saved.id());
        return toResponse(saved, author, assignee);
    }

    /** El botón "actualizar" del frontend: cambia la modificación, no la creación. */
    @Transactional
    public SiteLogRecordResponse touch(UUID tenantId, UUID projectId, UUID entryId,
                                       AuthenticatedUser user) {
        requireProjectAccess(tenantId, projectId, user);
        SiteLogRecord current = requireRecord(tenantId, projectId, entryId);
        requireEditPermission(tenantId, current, user);
        SiteLogRecord saved = repository.save(current.touched(LocalDateTime.now()));
        notifyChange(tenantId, projectId, user.userId(), saved.id());
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID tenantId, UUID projectId, UUID entryId, AuthenticatedUser user) {
        requireProjectAccess(tenantId, projectId, user);
        SiteLogRecord current = requireRecord(tenantId, projectId, entryId);
        requireEditPermission(tenantId, current, user);
        repository.deleteById(current.id(), tenantId);
        notifyChange(tenantId, projectId, user.userId(), current.id());
    }

    private void notifyChange(UUID tenantId, UUID projectId, UUID userId, UUID entryId) {
        events.publishEvent(ProjectChanged.of(tenantId, projectId,
                ProjectChanged.SITE_LOG, entryId, userId));
    }

    private void requireProjectAccess(UUID tenantId, UUID projectId, AuthenticatedUser user) {
        projects.ensureBelongsToTenant(projectId, tenantId);
        if (!user.role().canViewAllProjects()
                && !projects.isUserAssignedToProject(projectId, tenantId, user.userId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado");
        }
    }

    private SiteLogRecord requireRecord(UUID tenantId, UUID projectId, UUID entryId) {
        SiteLogRecord record = repository.findById(entryId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Entrada de bitácora no encontrada"));
        if (!record.projectId().equals(projectId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Entrada de bitácora no encontrada");
        }
        return record;
    }

    /**
     * Puede editar el autor o cualquier usuario con el permiso efectivo de
     * bitácora (ADMIN por rol, WORKER/MEMBER si un ADMIN se lo concedió).
     */
    private void requireEditPermission(UUID tenantId, SiteLogRecord record, AuthenticatedUser user) {
        if (record.authorId().equals(user.userId())) {
            return;
        }
        siteLogPermissions.requireCanManageSiteLog(tenantId, user.userId());
    }

    private UserSummary requireAssignee(UUID tenantId, UUID assigneeId) {
        if (assigneeId == null) {
            return null;
        }
        return users.ensureBelongsToTenant(assigneeId, tenantId);
    }

    private SiteLogRecordResponse toResponse(SiteLogRecord record) {
        UserSummary author = users.ensureBelongsToTenant(record.authorId(), record.tenantId());
        UserSummary assignee = record.assigneeId() == null
                ? null
                : users.ensureBelongsToTenant(record.assigneeId(), record.tenantId());
        return toResponse(record, author, assignee);
    }

    private SiteLogRecordResponse toResponse(SiteLogRecord record, UserSummary author,
                                             UserSummary assignee) {
        String projectName = projects.getProjectName(record.projectId(), record.tenantId());
        return SiteLogRecordResponse.from(record, projectName, author.fullName(),
                assignee == null ? null : assignee.fullName());
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
