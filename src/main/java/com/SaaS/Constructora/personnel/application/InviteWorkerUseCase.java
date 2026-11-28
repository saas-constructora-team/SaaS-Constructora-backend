package com.SaaS.Constructora.personnel.application;
import com.SaaS.Constructora.personnel.infrastructure.Worker;
import com.SaaS.Constructora.personnel.infrastructure.WorkerPosition;
import com.SaaS.Constructora.personnel.infrastructure.WorkerPositionRepository;
import com.SaaS.Constructora.personnel.infrastructure.WorkerRepository;
import com.SaaS.Constructora.personnel.interfaces.InviteWorkerRequest;
import com.SaaS.Constructora.personnel.interfaces.InviteWorkerResponse;

import com.SaaS.Constructora.identity.application.InvitationIssued;
import com.SaaS.Constructora.identity.application.InviteMembershipUseCase;
import com.SaaS.Constructora.identity.application.ports.UserRepository;
import com.SaaS.Constructora.identity.domain.User;
import com.SaaS.Constructora.identity.application.PersonnelPermissionService;
import com.SaaS.Constructora.projects.application.ports.ProjectTenantPort;
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
    private final ProjectTenantPort projectTenantPort;
    private final PersonnelPermissionService permissionService;
    private final InviteMembershipUseCase inviteMembershipUseCase;
    private final UserRepository userRepository;

    public InviteWorkerUseCase(WorkerRepository repository,
                               WorkerPositionRepository positionRepository,
                               ProjectTenantPort projectTenantPort,
                               PersonnelPermissionService permissionService,
                               InviteMembershipUseCase inviteMembershipUseCase,
                               UserRepository userRepository) {
        this.repository = repository;
        this.positionRepository = positionRepository;
        this.projectTenantPort = projectTenantPort;
        this.permissionService = permissionService;
        this.inviteMembershipUseCase = inviteMembershipUseCase;
        this.userRepository = userRepository;
    }

    @Transactional
    public InviteWorkerResponse invite(UUID tenantId, UUID actorId, InviteWorkerRequest request) {
        permissionService.requireCanManagePersonnel(tenantId, actorId);

        String documentNumber = requireText(request.documentNumber());
        if (request.projectId() != null) {
            projectTenantPort.ensureBelongsToTenant(request.projectId(), tenantId);
        }
        String positionName = resolvePositionName(tenantId, request.position());
        String username = User.normalizeUsername(request.username());
        if (username != null && userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El username ya está en uso");
        }

        InvitationIssued issued = inviteMembershipUseCase.invite(
                tenantId, actorId, request.email(), request.fullName(), username, request.role());

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
            worker.setUsername(issued.username());
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

        Worker saved = repository.save(worker);
        saved.setUsername(issued.username());
        return InviteWorkerResponse.from(saved, issued);
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
