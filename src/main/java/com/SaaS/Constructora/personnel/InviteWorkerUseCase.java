package com.SaaS.Constructora.personnel;

import com.SaaS.Constructora.identity.application.InvitationIssued;
import com.SaaS.Constructora.identity.application.InviteMembershipUseCase;
import com.SaaS.Constructora.identity.application.PersonnelPermissionService;
import com.SaaS.Constructora.projects.ProjectService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

/**
 * Invita a un trabajador y crea (o reutiliza) su ficha de personal.
 * <p>
 * El vínculo de identidad (User + Membership INVITED + token) lo resuelve el
 * módulo identity; este caso de uso solo aporta los datos laborales de
 * personnel y enlaza la ficha con el usuario invitado.
 */
@Service
public class InviteWorkerUseCase {

    private final WorkerRepository repository;
    private final WorkerPositionRepository positionRepository;
    private final ProjectService projectService;
    private final PersonnelPermissionService permissionService;
    private final InviteMembershipUseCase inviteMembershipUseCase;

    public InviteWorkerUseCase(WorkerRepository repository,
                               WorkerPositionRepository positionRepository,
                               ProjectService projectService,
                               PersonnelPermissionService permissionService,
                               InviteMembershipUseCase inviteMembershipUseCase) {
        this.repository = repository;
        this.positionRepository = positionRepository;
        this.projectService = projectService;
        this.permissionService = permissionService;
        this.inviteMembershipUseCase = inviteMembershipUseCase;
    }

    @Transactional
    public InviteWorkerResponse invite(UUID tenantId, UUID actorId, InviteWorkerRequest request) {
        permissionService.requireCanManagePersonnel(tenantId, actorId);

        String documentNumber = requireText(request.documentNumber());
        if (request.projectId() != null) {
            projectService.ensureBelongsToTenant(request.projectId(), tenantId);
        }
        String positionName = resolvePositionName(tenantId, request.position());

        InvitationIssued issued = inviteMembershipUseCase.invite(
                tenantId, actorId, request.email(), request.fullName(), request.role());

        Optional<Worker> existing = repository
                .findByTenantIdAndDocumentTypeAndDocumentNumber(
                        tenantId, request.documentType(), documentNumber);

        if (existing.isPresent()) {
            Worker worker = existing.get();
            if (worker.getUserId() != null && !worker.getUserId().equals(issued.userId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Ya existe un trabajador con ese documento vinculado a otra cuenta");
            }
            if (worker.getUserId() == null) {
                worker.linkToUser(issued.userId());
                worker = repository.save(worker);
            }
            return InviteWorkerResponse.from(worker, issued);
        }

        Worker worker = new Worker(
                tenantId,
                requireText(request.fullName()),
                request.documentType(),
                documentNumber,
                requireText(request.phone()),
                requireText(request.email()).toLowerCase(),
                null, // El flujo nuevo no duplica la contraseña en workers
                issued.userId(),
                requireText(request.emergencyContactName()),
                requireText(request.emergencyContactPhone()),
                positionName,
                request.projectId());

        return InviteWorkerResponse.from(repository.save(worker), issued);
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

    private static String requireText(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Faltan datos obligatorios del trabajador");
        }
        return value.trim();
    }
}
