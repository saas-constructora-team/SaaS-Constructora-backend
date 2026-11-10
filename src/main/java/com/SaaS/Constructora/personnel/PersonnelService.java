package com.SaaS.Constructora.personnel;

import com.SaaS.Constructora.identity.application.PersonnelPermissionService;
import com.SaaS.Constructora.projects.ProjectService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Casos de uso de gestión del personal (trabajadores) de la empresa.
 * <p>
 * Toda operación se acota al tenant del token: el proyecto y los trabajadores
 * de otra empresa se reportan como inexistentes (404). Puede operar un ADMIN o
 * un MEMBER al que un ADMIN le haya concedido el permiso de creación de personal.
 */
@Service
public class PersonnelService {

    private final WorkerRepository repository;
    private final WorkerPositionRepository positionRepository;
    private final ProjectService projectService;
    private final PersonnelPermissionService permissionService;

    public PersonnelService(WorkerRepository repository, WorkerPositionRepository positionRepository,
                            ProjectService projectService, PersonnelPermissionService permissionService) {
        this.repository = repository;
        this.positionRepository = positionRepository;
        this.projectService = projectService;
        this.permissionService = permissionService;
    }

    /**
     * Registra un trabajador en la empresa. Si se indica una obra, se valida
     * que pertenezca al mismo tenant antes de vincularla.
     * <p>
     * El cargo se especifica por nombre. Si aún no existe en el catálogo del
     * tenant, se registra automáticamente para que pueda gestionarse después.
     */
    @Transactional
    public WorkerResponse create(UUID tenantId, UUID actorId, CreateWorkerRequest request) {
        permissionService.requireCanManagePersonnel(tenantId, actorId);

        String documentNumber = requireText(request.documentNumber());
        if (repository.existsByTenantIdAndDocumentTypeAndDocumentNumber(
                tenantId, request.documentType(), documentNumber)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un trabajador con ese documento en la empresa");
        }

        if (request.projectId() != null) {
            projectService.ensureBelongsToTenant(request.projectId(), tenantId);
        }

        String positionName = resolvePositionName(tenantId, request.position());

        Worker worker = new Worker(
                tenantId,
                requireText(request.fullName()),
                request.documentType(),
                documentNumber,
                requireText(request.phone()),
                optionalText(request.email()),
                requireText(request.emergencyContactName()),
                requireText(request.emergencyContactPhone()),
                positionName,
                request.projectId());

        return WorkerResponse.from(repository.save(worker));
    }

    /**
     * Resuelve el cargo por nombre dentro del tenant. Si no existe, lo crea en
     * el catálogo; si existe pero está inactivo, rechaza la asignación.
     */
    private String resolvePositionName(UUID tenantId, String position) {
        if (position == null || position.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El cargo del trabajador es obligatorio");
        }

        String name = position.trim();
        WorkerPosition existing = positionRepository
                .findByTenantIdAndName(tenantId, name)
                .orElse(null);

        if (existing != null) {
            if (!existing.isActive()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El cargo '" + name + "' está inactivo en la empresa");
            }
            return existing.getName();
        }

        WorkerPosition created = positionRepository.save(
                WorkerPosition.create(tenantId, name, null));
        return created.getName();
    }

    /** Lista el personal de la empresa, del más reciente al más antiguo. */
    @Transactional(readOnly = true)
    public List<WorkerResponse> list(UUID tenantId, UUID actorId) {
        permissionService.requireCanManagePersonnel(tenantId, actorId);
        return repository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(WorkerResponse::from)
                .toList();
    }

    private static String requireText(String value) {
        return value.trim();
    }

    /** Normaliza los datos opcionales: recorta y convierte vacíos en null. */
    private static String optionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
