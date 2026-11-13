package com.SaaS.Constructora.identity.interfaces;

import com.SaaS.Constructora.identity.application.AcceptInvitationUseCase;
import com.SaaS.Constructora.identity.application.GoogleAuthUseCase;
import com.SaaS.Constructora.identity.application.InvitationQueryService;
import com.SaaS.Constructora.identity.application.LoginUseCase;
import com.SaaS.Constructora.identity.application.RegisterTenantUseCase;
import com.SaaS.Constructora.identity.application.RequestPasswordResetUseCase;
import com.SaaS.Constructora.identity.application.ResetPasswordUseCase;
import com.SaaS.Constructora.identity.interfaces.dto.AcceptInvitationRequest;
import com.SaaS.Constructora.identity.interfaces.dto.ForgotPasswordRequest;
import com.SaaS.Constructora.identity.interfaces.dto.GoogleAuthRequest;
import com.SaaS.Constructora.identity.interfaces.dto.GoogleAuthResponse;
import com.SaaS.Constructora.identity.interfaces.dto.InvitationPreviewResponse;
import com.SaaS.Constructora.identity.interfaces.dto.LoginRequest;
import com.SaaS.Constructora.identity.interfaces.dto.LoginResponse;
import com.SaaS.Constructora.identity.interfaces.dto.MessageResponse;
import com.SaaS.Constructora.identity.interfaces.dto.RegisterTenantRequest;
import com.SaaS.Constructora.identity.interfaces.dto.RegisterTenantResponse;
import com.SaaS.Constructora.identity.interfaces.dto.ResetPasswordRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final LoginUseCase loginUseCase;
    private final RegisterTenantUseCase registerTenantUseCase;
    private final GoogleAuthUseCase googleAuthUseCase;
    private final RequestPasswordResetUseCase requestPasswordResetUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final AcceptInvitationUseCase acceptInvitationUseCase;
    private final InvitationQueryService invitationQueryService;

    AuthController(LoginUseCase loginUseCase, RegisterTenantUseCase registerTenantUseCase,
                   GoogleAuthUseCase googleAuthUseCase,
                   RequestPasswordResetUseCase requestPasswordResetUseCase,
                   ResetPasswordUseCase resetPasswordUseCase,
                   AcceptInvitationUseCase acceptInvitationUseCase,
                   InvitationQueryService invitationQueryService) {
        this.loginUseCase = loginUseCase;
        this.registerTenantUseCase = registerTenantUseCase;
        this.googleAuthUseCase = googleAuthUseCase;
        this.requestPasswordResetUseCase = requestPasswordResetUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
        this.acceptInvitationUseCase = acceptInvitationUseCase;
        this.invitationQueryService = invitationQueryService;
    }

    @PostMapping("/login")
    ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(loginUseCase.login(request));
    }

    @PostMapping("/register")
    ResponseEntity<RegisterTenantResponse> register(@Valid @RequestBody RegisterTenantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registerTenantUseCase.register(request));
    }

    // HU-01: inicio de sesión o alta con Google. 200 si la cuenta ya existía,
    // 201 si en esta misma petición se creó la empresa + usuario.
    @PostMapping("/google")
    ResponseEntity<GoogleAuthResponse> google(@Valid @RequestBody GoogleAuthRequest request) {
        GoogleAuthResponse response = googleAuthUseCase.authenticate(request);
        return ResponseEntity.status(response.newUser() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(response);
    }

    // HU-13: responde 200 siempre, exista o no el email (anti user-enumeration)
    @PostMapping("/forgot-password")
    ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        requestPasswordResetUseCase.request(request.email());
        return ResponseEntity.ok(new MessageResponse(
                "Si el correo está registrado, recibirás un enlace para restablecer tu contraseña."));
    }

    @PostMapping("/reset-password")
    ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        resetPasswordUseCase.reset(request.token(), request.newPassword());
        return ResponseEntity.ok(new MessageResponse("Contraseña actualizada correctamente."));
    }

    /** Previsualización pública de una invitación (sin autenticación). */
    @GetMapping("/invitations/{token}")
    ResponseEntity<InvitationPreviewResponse> previewInvitation(@PathVariable String token) {
        return ResponseEntity.ok(
                InvitationPreviewResponse.from(invitationQueryService.preview(token)));
    }

    /** Acepta la invitación y devuelve una sesión del tenant invitado. */
    @PostMapping("/invitations/accept")
    ResponseEntity<LoginResponse> acceptInvitation(
            @Valid @RequestBody AcceptInvitationRequest request) {
        return ResponseEntity.ok(acceptInvitationUseCase.accept(request));
    }
}
