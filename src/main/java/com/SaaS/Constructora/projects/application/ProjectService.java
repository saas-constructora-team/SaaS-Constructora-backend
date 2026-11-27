package com.SaaS.Constructora.projects.application;

import com.SaaS.Constructora.documents.application.ports.FileUpload;
import com.SaaS.Constructora.documents.application.ports.ObjectStoragePort;
import com.SaaS.Constructora.documents.domain.AssetKind;
import com.SaaS.Constructora.documents.domain.FilePolicy;
import com.SaaS.Constructora.identity.application.TenantQueryService;
import com.SaaS.Constructora.identity.domain.Role;
import com.SaaS.Constructora.projects.application.ports.ProjectAssignmentRepository;
import com.SaaS.Constructora.projects.application.ports.ProjectTenantPort;
import com.SaaS.Constructora.projects.domain.ProjectPhoto;
import com.SaaS.Constructora.projects.domain.ProjectStatus;
import com.SaaS.Constructora.projects.domain.ProjectType;
import com.SaaS.Constructora.projects.infrastructure.Project;
import com.SaaS.Constructora.projects.infrastructure.ProjectArea;
import com.SaaS.Constructora.projects.infrastructure.ProjectAreaRepository;
import com.SaaS.Constructora.projects.infrastructure.ProjectPhotoEntity;
import com.SaaS.Constructora.projects.infrastructure.ProjectPhotoRepository;
import com.SaaS.Constructora.projects.infrastructure.ProjectRepository;
import com.SaaS.Constructora.projects.infrastructure.ProjectStage;
import com.SaaS.Constructora.projects.infrastructure.ProjectStageRepository;
import com.SaaS.Constructora.projects.infrastructure.TaskStatus;
import com.SaaS.Constructora.projects.infrastructure.TaskStatusRepository;
import com.SaaS.Constructora.projects.interfaces.CreateProjectRequest;
import com.SaaS.Constructora.projects.interfaces.ProjectResponse;
import com.SaaS.Constructora.projects.interfaces.UpdateProjectRequest;
import com.SaaS.Constructora.shared.events.ProjectChanged;
import com.SaaS.Constructora.shared.events.ProjectCreated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.PushbackInputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ProjectService implements ProjectTenantPort {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int HEADER_BYTES = 16;
    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository repository;
    private final TenantQueryService tenantQueryService;
    private final ProjectAssignmentRepository assignmentRepository;
    private final ProjectStageRepository stageRepository;
    private final TaskStatusRepository statusRepository;
    private final ProjectAreaRepository areaRepository;
    private final ApplicationEventPublisher events;
    private final ProjectPhotoRepository photoRepository;
    private final ObjectStoragePort storage;

    public ProjectService(ProjectRepository repository, TenantQueryService tenantQueryService,
                          ProjectAssignmentRepository assignmentRepository,
                          ProjectStageRepository stageRepository,
                          TaskStatusRepository statusRepository,
                          ProjectAreaRepository areaRepository,
                          ApplicationEventPublisher events,
                          ProjectPhotoRepository photoRepository,
                          ObjectStoragePort storage) {
        this.repository = repository;
        this.tenantQueryService = tenantQueryService;
        this.assignmentRepository = assignmentRepository;
        this.stageRepository = stageRepository;
        this.statusRepository = statusRepository;
        this.areaRepository = areaRepository;
        this.events = events;
        this.photoRepository = photoRepository;
        this.storage = storage;
    }

    @Transactional
    public ProjectResponse create(UUID tenantId, UUID userId, Role role, CreateProjectRequest req) {
        requireCanCreate(role);
        // Validación contextual: la empresa debe existir antes de asociarle proyectos.
        tenantQueryService.ensureExists(tenantId);
        Project project = new Project(tenantId, req.toDetails());
        Project saved = repository.save(project);
        stageRepository.saveAll(ProjectStage.initialStages(tenantId, saved.getId()));
        statusRepository.saveAll(TaskStatus.initialStatuses(tenantId, saved.getId()));
        areaRepository.saveAll(ProjectArea.initialAreas(tenantId, saved.getId()));
        events.publishEvent(new ProjectCreated(
                UUID.randomUUID(), tenantId, saved.getId(), userId,
                saved.getName(), saved.getCreatedAt()));
        return ProjectResponse.from(saved);
    }

    /**
     * ADMIN (y futuros roles altos) ve todos los proyectos de la empresa.
     * El resto solo ve los proyectos que tiene asignados. La visibilidad se
     * resuelve en una única consulta con el tenant incluido.
     */
    @Transactional(readOnly = true)
    public List<ProjectResponse> list(UUID tenantId, UUID userId, Role role) {
        return repository.findVisibleProjects(tenantId, userId, role.canViewAllProjects())
                .stream()
                .map(ProjectResponse::from)
                .toList();
    }

    /** Búsqueda paginada con filtros opcionales, restringida al tenant y a la visibilidad. */
    @Transactional(readOnly = true)
    public Page<ProjectResponse> search(UUID tenantId, UUID userId, Role role, String query,
                                        ProjectStatus status, ProjectType projectType,
                                        LocalDateTime from, LocalDateTime to, Pageable pageable) {
        String namePattern = (query == null || query.isBlank())
                ? null
                : "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
        Sort allowedSort = Sort.by(Sort.Direction.DESC, "createdAt")
                .and(Sort.by(Sort.Direction.DESC, "id"));
        PageRequest safePage = PageRequest.of(
                Math.max(pageable.getPageNumber(), 0),
                Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE),
                allowedSort);
        return repository.searchProjects(tenantId, userId, role.canViewAllProjects(),
                        namePattern, status, projectType, from, to, safePage)
                .map(ProjectResponse::from);
    }

    /**
     * API pública del módulo para que otros casos de uso validen que un
     * proyecto pertenece al tenant antes de operar sobre él.
     */
    @Transactional(readOnly = true)
    public void ensureBelongsToTenant(UUID projectId, UUID tenantId) {
        requireProjectOfTenant(tenantId, projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public String getProjectName(UUID projectId, UUID tenantId) {
        return requireProjectOfTenant(tenantId, projectId).getName();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUserAssignedToProject(UUID projectId, UUID tenantId, UUID userId) {
        return assignmentRepository.existsActiveByTenantIdAndProjectIdAndUserId(
                tenantId, projectId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public void ensureStageBelongsToProjectAndTenant(UUID stageId, UUID projectId, UUID tenantId) {
        stageRepository.findByIdAndTenantIdAndProjectId(stageId, tenantId, projectId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Etapa no encontrada"));
    }

    @Override
    @Transactional(readOnly = true)
    public void ensureStageAllowsEntries(UUID stageId) {
        ProjectStage stage = stageRepository.findById(stageId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Etapa no encontrada"));
        if (stage.isClosed()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "La etapa está cerrada, no permite registrar novedades");
        }
    }

    @Override
    @Transactional
    public ProjectPhoto registerPhoto(UUID tenantId, UUID projectId, UUID stageId,
                                      UUID userId, Role role, FileUpload file) {
        requireAccessibleProject(projectId, tenantId, userId, role);
        ensureStageBelongsToProjectAndTenant(stageId, projectId, tenantId);
        ensureStageAllowsEntries(stageId);
        if (file == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo es obligatorio");
        }

        UUID photoId = UUID.randomUUID();
        String name = safeFileName(file.originalName());
        String contentType = safeContentType(file.contentType());
        String key = "tenants/" + tenantId + "/projects/" + projectId + "/photos/"
                + stageId + "/" + photoId + "/" + name;
        MessageDigest digest = sha256Digest();
        byte[] header = new byte[HEADER_BYTES];
        try {
            String sha256;
            try (PushbackInputStream probe = new PushbackInputStream(file.content(), HEADER_BYTES);
                 DigestInputStream content = new DigestInputStream(probe, digest)) {
                int read = probe.read(header);
                FilePolicy.validate(AssetKind.IMAGE, file.originalName(), file.contentType(),
                        file.size(), read < 0 ? new byte[0] : Arrays.copyOf(header, read));
                if (read > 0) {
                    probe.unread(header, 0, read);
                }
                storage.store(key, content, file.size(), contentType);
                sha256 = HexFormat.of().formatHex(digest.digest());
            }
            ProjectPhoto photo = new ProjectPhoto(photoId, tenantId, projectId, stageId,
                    key, contentType, file.size(), sha256, name, userId, LocalDateTime.now());
            photoRepository.save(new ProjectPhotoEntity(photo));
            events.publishEvent(ProjectChanged.of(tenantId, projectId,
                    ProjectChanged.DOCUMENT, photo.id(), userId));
            return photo;
        } catch (IOException exception) {
            compensate(key);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se pudo leer el archivo", exception);
        } catch (RuntimeException exception) {
            compensate(key);
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<ProjectPhoto> listPhotos(UUID tenantId, UUID projectId, UUID stageId,
                                         UUID userId, Role role) {
        requireAccessibleProject(projectId, tenantId, userId, role);
        ensureStageBelongsToProjectAndTenant(stageId, projectId, tenantId);
        return photoRepository.findByTenantIdAndProjectIdAndStageIdOrderByCreatedAtDesc(
                        tenantId, projectId, stageId)
                .stream()
                .map(ProjectPhotoEntity::toDomain)
                .toList();
    }

    /**
     * Valida que el proyecto exista en el tenant y que el usuario pueda
     * acceder a él. ADMIN accede a todos; MEMBER solo a los asignados.
     * Un proyecto no asignado se reporta como inexistente (404) para no
     * revelar su existencia.
     */
    @Transactional(readOnly = true)
    public void ensureUserCanAccessProject(UUID projectId, UUID tenantId,
                                           UUID userId, Role role) {
        requireAccessibleProject(projectId, tenantId, userId, role);
    }

    /**
     * ADMIN edita cualquier proyecto de su empresa; un MEMBER solo los que
     * tiene asignados. El proyecto se resuelve una sola vez para aplicar el
     * aislamiento de tenant y la autorización sobre la misma entidad.
     */
    @Transactional
    public ProjectResponse update(UUID tenantId, UUID projectId, UUID userId, Role role,
                                  UpdateProjectRequest req) {
        Project project = requireAccessibleProject(projectId, tenantId, userId, role);
        project.updateDetails(req.toDetails());
        events.publishEvent(ProjectChanged.of(tenantId, projectId,
                ProjectChanged.PROJECT, projectId, userId));
        return ProjectResponse.from(project);
    }

    @Transactional
    public void delete(UUID tenantId, UUID projectId, UUID userId, Role role) {
        requireCanDelete(role);
        repository.delete(requireProjectOfTenant(tenantId, projectId));
        events.publishEvent(ProjectChanged.of(tenantId, projectId,
                ProjectChanged.PROJECT, projectId, userId));
    }

    private void requireCanCreate(Role role) {
        if (!role.canCreateProject()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Se requiere rol de administrador para crear proyectos");
        }
    }

    private void requireCanDelete(Role role) {
        if (!role.canDeleteProject()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Se requiere rol de administrador para eliminar proyectos");
        }
    }

    /**
     * Valida tenant y acceso en una sola resolución del proyecto. Un proyecto
     * no asignado se reporta como inexistente (404) para no revelar su
     * existencia a un MEMBER.
     */
    private Project requireAccessibleProject(UUID projectId, UUID tenantId,
                                             UUID userId, Role role) {
        Project project = requireProjectOfTenant(tenantId, projectId);
        if (!role.canViewAllProjects()
                && !assignmentRepository.existsActiveByTenantIdAndProjectIdAndUserId(
                        tenantId, projectId, userId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Proyecto no encontrado");
        }
        return project;
    }

    /**
     * Un proyecto de otra empresa se reporta como inexistente (404) para no
     * revelar que existe fuera del tenant autenticado.
     */
    private Project requireProjectOfTenant(UUID tenantId, UUID projectId) {
        return repository.findByIdAndTenantId(projectId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Proyecto no encontrado"));
    }

    private MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no disponible en esta JVM", exception);
        }
    }

    /** Si falla la transacción, solo hay que compensar el objeto subido a S3. */
    private void compensate(String key) {
        try {
            storage.delete(key);
        } catch (RuntimeException exception) {
            log.warn("No se pudo eliminar el objeto {} tras un fallo al registrar la foto",
                    key, exception);
        }
    }

    /** Nombre seguro para la clave S3 y para el nombre original persistido. */
    private String safeFileName(String originalName) {
        String name = originalName == null ? "foto" : originalName.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        name = Normalizer.normalize(name, Normalizer.Form.NFKD).replaceAll("\\p{M}", "");
        name = name.replaceAll("[^A-Za-z0-9._-]", "_");
        name = name.replaceAll("_{2,}", "_");
        while (name.startsWith(".")) {
            name = name.substring(1);
        }
        if (name.isBlank()) {
            name = "foto";
        }
        return name.length() > 100 ? name.substring(name.length() - 100) : name;
    }

    private String safeContentType(String contentType) {
        return contentType == null || contentType.isBlank()
                ? "application/octet-stream"
                : contentType;
    }
}
