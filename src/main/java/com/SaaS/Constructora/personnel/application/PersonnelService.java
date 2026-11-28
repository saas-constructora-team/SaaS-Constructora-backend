package com.SaaS.Constructora.personnel.application;
import com.SaaS.Constructora.personnel.infrastructure.Worker;
import com.SaaS.Constructora.personnel.infrastructure.WorkerPosition;
import com.SaaS.Constructora.personnel.infrastructure.WorkerPositionRepository;
import com.SaaS.Constructora.personnel.infrastructure.WorkerRepository;
import com.SaaS.Constructora.personnel.interfaces.CreateWorkerRequest;
import com.SaaS.Constructora.personnel.interfaces.WorkerResponse;

import com.SaaS.Constructora.identity.application.PersonnelPermissionService;
import com.SaaS.Constructora.identity.application.ports.MembershipRepository;
import com.SaaS.Constructora.identity.application.ports.UserRepository;
import com.SaaS.Constructora.identity.domain.Membership;
import com.SaaS.Constructora.identity.domain.User;
import com.SaaS.Constructora.identity.domain.Role;
import com.SaaS.Constructora.projects.application.ports.ProjectTenantPort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
    private final ProjectTenantPort projectTenantPort;
    private final PersonnelPermissionService permissionService;
    private final PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;

    public PersonnelService(WorkerRepository repository, WorkerPositionRepository positionRepository,
                            ProjectTenantPort projectTenantPort, PersonnelPermissionService permissionService,
                            PasswordEncoder passwordEncoder, UserRepository userRepository,
                            MembershipRepository membershipRepository) {
        this.repository = repository;
        this.positionRepository = positionRepository;
        this.projectTenantPort = projectTenantPort;
        this.permissionService = permissionService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
    }

    /**
     * Registra un trabajador en la empresa. Si se indica una obra, se valida
     * que pertenezca al mismo tenant antes de vincularla.
     * <p>
     * El cargo se especifica por nombre. Si aún no existe en el catálogo del
     * tenant, se registra automáticamente para que pueda gestionarse después.
     * La contraseña se hashea con BCrypt y se guarda como password_hash para
     * permitir el inicio de sesión del personal. Al crear el worker, también se
     * crea un registro User + Membership en el módulo identity, vinculando al
     * trabajador con credenciales de acceso al sistema.
     * <p>
     * <b>Flujo heredado:</b> conserva la creación directa con contraseña por
     * compatibilidad con clientes existentes. Para altas nuevas use
     * {@link InviteWorkerUseCase}, donde la persona define su propia contraseña.
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
            projectTenantPort.ensureBelongsToTenant(request.projectId(), tenantId);
        }

        // Contraseña se ha validado obligatoriamente en el DTO; hashearla con BCrypt.
        String passwordHash = passwordEncoder.encode(request.password());

        // Verificar si ya existe un usuario con ese email (para evitar duplicados)
        Optional<User> existingUser = userRepository.findByEmail(request.email());
        if (existingUser.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un usuario con ese correo electrónico en el sistema");
        }

        String username = User.normalizeUsername(request.username());
        if (username != null && userRepository.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El username ya está en uso");
        }

        // Crear el usuario de identidad
        User user = new User(
                UUID.randomUUID(),
                request.email(),
                requireText(request.fullName()),
                username,
                passwordHash,
                true, // active
                LocalDateTime.now()
        );
        user = userRepository.save(user);

        // Crear la membresía del worker en el tenant con rol WORKER (mínimos)
        Membership membership = new Membership(
                UUID.randomUUID(),      // membership id
                user.getId(),           // userId
                tenantId,               // tenantId
                Role.WORKER,            // role: mínimo para login y permisos restrictivos
                true,                   // active
                false,                  // personnelCreationAllowed: false por defecto
                LocalDateTime.now()     // createdAt
        );
        membershipRepository.save(membership);

        String positionName = resolvePositionName(tenantId, request.position());

        Worker worker = new Worker(
                tenantId,
                requireText(request.fullName()),
                request.documentType(),
                documentNumber,
                requireText(request.phone()),
                requireText(request.email()),
                passwordHash,
                user.getId(),           // userId: vincula al worker con su usuario de auth
                requireText(request.emergencyContactName()),
                requireText(request.emergencyContactPhone()),
                positionName,
                request.projectId());

        Worker saved = repository.save(worker);
        saved.setUsername(user.getUsername());
        return WorkerResponse.from(saved);
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
        List<Worker> workers = repository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        for (Worker worker : workers) {
            if (worker.getUserId() != null) {
                userRepository.findById(worker.getUserId()).ifPresent(u -> worker.setUsername(u.getUsername()));
            }
        }
        return workers.stream().map(WorkerResponse::from).toList();
    }

    private static String requireText(String value) {
        return value.trim();
    }
}
