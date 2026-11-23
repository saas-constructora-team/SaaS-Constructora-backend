package com.SaaS.Constructora.personnel;
import com.SaaS.Constructora.personnel.application.PersonnelService;
import com.SaaS.Constructora.personnel.domain.DocumentType;
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
import com.SaaS.Constructora.projects.application.ports.ProjectTenantPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PersonnelServiceTest {

    private final WorkerRepository repository = mock(WorkerRepository.class);
    private final WorkerPositionRepository positionRepository =
            mock(WorkerPositionRepository.class);
    private final ProjectTenantPort projectTenantPort = mock(ProjectTenantPort.class);
    private final PersonnelPermissionService permissionService =
            mock(PersonnelPermissionService.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final MembershipRepository membershipRepository = mock(MembershipRepository.class);
    private final PersonnelService service =
            new PersonnelService(repository, positionRepository, projectTenantPort, permissionService, passwordEncoder, userRepository, membershipRepository);

private static User anyUser() {
        return new User(UUID.randomUUID(), "ana@constructora.com",
                "Ana Gómez Pérez", "hashed-pw", true, LocalDateTime.now());
    }

    @BeforeEach
    void setUp() {
        when(positionRepository.save(any(WorkerPosition.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-pw");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doAnswer(invocation -> null).when(membershipRepository).save(any(Membership.class));
    }

    private static CreateWorkerRequest request(UUID projectId) {
        return new CreateWorkerRequest(
                "Ana Gómez Pérez",
                DocumentType.NATIONAL_ID,
                "1020304050",
                "+57 300 123 4567",
                "ana@constructora.com",
                "Admin123!",
                "Carlos Gómez",
                "+57 300 765 4321",
                "Encargado de obra",
                projectId);
    }

    @Test
    void registraUnTrabajadorDelTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        when(repository.save(any(Worker.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WorkerResponse response = service.create(tenantId, actorId, request(projectId));

        verify(permissionService).requireCanManagePersonnel(tenantId, actorId);
        verify(projectTenantPort).ensureBelongsToTenant(projectId, tenantId);
        assertEquals("Ana Gómez Pérez", response.fullName());
        assertEquals("NATIONAL_ID", response.documentType());
        assertEquals("1020304050", response.documentNumber());
        assertEquals("Encargado de obra", response.position());
        assertEquals(projectId, response.projectId());
    }

    @Test
    void creaElCargoSiNoExisteEnElCatalogo() {
        UUID tenantId = UUID.randomUUID();
        when(repository.save(any(Worker.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.create(tenantId, UUID.randomUUID(), request(null));

        verify(positionRepository).save(any(WorkerPosition.class));
    }

    @Test
    void noAsignaUnCargoInactivo() {
        UUID tenantId = UUID.randomUUID();
        WorkerPosition position = WorkerPosition.create(tenantId, "Encargado de obra", null);
        position.activate(false);
        when(positionRepository.findByTenantIdAndName(tenantId, "Encargado de obra"))
                .thenReturn(Optional.of(position));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(tenantId, UUID.randomUUID(), request(null)));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(repository, never()).save(any(Worker.class));
    }

    @Test
    void permiteRegistrarSinObraAsignada() {
        UUID tenantId = UUID.randomUUID();
        when(repository.save(any(Worker.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WorkerResponse response = service.create(tenantId, UUID.randomUUID(), request(null));

        verifyNoInteractions(projectTenantPort);
        assertEquals(null, response.projectId());
    }

    @Test
    void noRegistraDocumentoDuplicadoEnLaEmpresa() {
        UUID tenantId = UUID.randomUUID();
        when(repository.existsByTenantIdAndDocumentTypeAndDocumentNumber(
                tenantId, DocumentType.NATIONAL_ID, "1020304050")).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(tenantId, UUID.randomUUID(), request(null)));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(repository, never()).save(any(Worker.class));
    }

    @Test
    void noVinculaUnaObraDeOtraEmpresa() {
        UUID tenantId = UUID.randomUUID();
        UUID projectId = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado"))
                .when(projectTenantPort).ensureBelongsToTenant(projectId, tenantId);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(tenantId, UUID.randomUUID(), request(projectId)));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(repository, never()).save(any(Worker.class));
    }

    @Test
    void noRegistraSinPermisoDePersonal() {
        UUID tenantId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Sin permisos"))
                .when(permissionService).requireCanManagePersonnel(tenantId, actorId);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(tenantId, actorId, request(null)));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(repository, never()).save(any(Worker.class));
    }

    @Test
    void listaElPersonalDeLaEmpresa() {
        UUID tenantId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Worker worker = new Worker(tenantId, "Ana Gómez", DocumentType.NATIONAL_ID,
                "123", "300", null, "pw-hash", userId, "Carlos", "301", "Operario", null);
        when(repository.findByTenantIdOrderByCreatedAtDesc(tenantId))
                .thenReturn(List.of(worker));

        List<WorkerResponse> workers = service.list(tenantId, actorId);

        verify(permissionService).requireCanManagePersonnel(tenantId, actorId);
        assertEquals(1, workers.size());
        assertEquals("Ana Gómez", workers.getFirst().fullName());
    }
}
