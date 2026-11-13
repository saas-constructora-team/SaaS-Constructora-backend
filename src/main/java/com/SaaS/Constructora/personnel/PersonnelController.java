package com.SaaS.Constructora.personnel;

import com.SaaS.Constructora.shared.security.AuthenticatedUser;
import com.SaaS.Constructora.shared.security.CurrentUser;
import com.SaaS.Constructora.identity.application.ResendInvitationUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Registro y listado del personal de la empresa autenticada, y gestión de sus
 * cargos (posiciones). El tenant y el usuario siempre se toman del JWT; nunca
 * del cuerpo de la petición. La autorización fina se valida en los servicios.
 */
@RestController
@RequestMapping("/api/personnel")
class PersonnelController {

    private final PersonnelService service;
    private final WorkerPositionService positionService;
    private final InviteWorkerUseCase inviteWorkerUseCase;
    private final ResendInvitationUseCase resendInvitationUseCase;

    PersonnelController(PersonnelService service, WorkerPositionService positionService,
                        InviteWorkerUseCase inviteWorkerUseCase,
                        ResendInvitationUseCase resendInvitationUseCase) {
        this.service = service;
        this.positionService = positionService;
        this.inviteWorkerUseCase = inviteWorkerUseCase;
        this.resendInvitationUseCase = resendInvitationUseCase;
    }

    /**
     * Invita a un trabajador. No recibe contraseña: la persona la define al
     * aceptar el enlace enviado a su correo.
     */
    @PostMapping("/invite")
    ResponseEntity<InviteWorkerResponse> invite(
            @CurrentUser AuthenticatedUser user,
            @Valid @RequestBody InviteWorkerRequest request) {
        InviteWorkerResponse response = inviteWorkerUseCase.invite(
                user.tenantId(), user.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Reenvía la invitación pendiente de una membresía. */
    @PostMapping("/invitations/{membershipId}/resend")
    ResponseEntity<InviteWorkerResponse> resendInvitation(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID membershipId) {
        return ResponseEntity.ok(InviteWorkerResponse.from(
                resendInvitationUseCase.resend(user.tenantId(), user.userId(), membershipId)));
    }

    /** Registra un nuevo trabajador en la empresa. */
    @PostMapping
    ResponseEntity<WorkerResponse> create(
            @CurrentUser AuthenticatedUser user,
            @Valid @RequestBody CreateWorkerRequest request) {
        WorkerResponse response = service.create(user.tenantId(), user.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Lista el personal de la empresa, del más reciente al más antiguo. */
    @GetMapping
    List<WorkerResponse> list(@CurrentUser AuthenticatedUser user) {
        return service.list(user.tenantId(), user.userId());
    }

    /** Crea un nuevo cargo en el tenant autenticado. */
    @PostMapping("/positions")
    ResponseEntity<WorkerPositionResponse> createPosition(
            @CurrentUser AuthenticatedUser user,
            @Valid @RequestBody CreateWorkerPositionRequest request) {
        WorkerPositionResponse response = positionService.create(
                user.tenantId(), user.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Lista todos los cargos del tenant autenticado. */
    @GetMapping("/positions")
    List<WorkerPositionResponse> listPositions(@CurrentUser AuthenticatedUser user) {
        return positionService.list(user.tenantId(), user.userId());
    }

    /** Actualiza un cargo existente. */
    @PutMapping("/positions/{id}")
    ResponseEntity<WorkerPositionResponse> updatePosition(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWorkerPositionRequest request) {
        WorkerPositionResponse response = positionService.update(
                user.tenantId(), user.userId(), id, request);
        return ResponseEntity.ok(response);
    }

    /** Elimina un cargo que no tenga trabajadores asignados. */
    @DeleteMapping("/positions/{id}")
    ResponseEntity<Void> deletePosition(@CurrentUser AuthenticatedUser user,
                                        @PathVariable UUID id) {
        positionService.delete(user.tenantId(), user.userId(), id);
        return ResponseEntity.noContent().build();
    }
}
