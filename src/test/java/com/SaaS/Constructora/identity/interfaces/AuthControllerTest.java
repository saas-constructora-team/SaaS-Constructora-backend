package com.SaaS.Constructora.identity.interfaces;

import com.SaaS.Constructora.identity.application.AcceptInvitationUseCase;
import com.SaaS.Constructora.identity.application.AuthenticationResult;
import com.SaaS.Constructora.identity.application.GoogleAuthResult;
import com.SaaS.Constructora.identity.application.GoogleAuthUseCase;
import com.SaaS.Constructora.identity.application.InvitationPreview;
import com.SaaS.Constructora.identity.application.InvitationQueryService;
import com.SaaS.Constructora.identity.application.LoginUseCase;
import com.SaaS.Constructora.identity.application.RegisterTenantResult;
import com.SaaS.Constructora.identity.application.RegisterTenantUseCase;
import com.SaaS.Constructora.identity.application.RequestPasswordResetUseCase;
import com.SaaS.Constructora.identity.application.ResetPasswordUseCase;
import com.SaaS.Constructora.identity.domain.Membership;
import com.SaaS.Constructora.identity.domain.Role;
import com.SaaS.Constructora.identity.domain.Tenant;
import com.SaaS.Constructora.identity.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // el SecurityConfig real no aplica en este slice
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoginUseCase loginUseCase;

    @MockitoBean
    private RegisterTenantUseCase registerTenantUseCase;

    @MockitoBean
    private GoogleAuthUseCase googleAuthUseCase;

    @MockitoBean
    private RequestPasswordResetUseCase requestPasswordResetUseCase;

    @MockitoBean
    private ResetPasswordUseCase resetPasswordUseCase;

    @MockitoBean
    private AcceptInvitationUseCase acceptInvitationUseCase;

    @MockitoBean
    private InvitationQueryService invitationQueryService;

    @Test
    void loginConCredenciasValidasDevuelve200YToken() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        User user = new User(userId, "admin@demo.com", "Administrador Demo", "hash", true, now);
        Membership membership = new Membership(
                UUID.randomUUID(), userId, tenantId, Role.ADMIN, true, now);
        when(loginUseCase.login(any()))
                .thenReturn(new AuthenticationResult("jwt-token", 3600, user, membership));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@demo.com\",\"password\":\"Admin123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600))
                .andExpect(jsonPath("$.user.email").value("admin@demo.com"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.user.tenantId").value(tenantId.toString()));
    }

    @Test
    void loginConCredenciasInvalidasDevuelve401() throws Exception {
        when(loginUseCase.login(any()))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@demo.com\",\"password\":\"mala\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginSinCamposDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registroDeEmpresaDevuelve201YToken() throws Exception {
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Tenant tenant = new Tenant(tenantId, "Constructora Nueva", "constructora-nueva", now);
        User admin = new User(userId, "jefe@nueva.com", "Jefe Nueva", "hash", true, now);
        Membership membership = new Membership(
                UUID.randomUUID(), userId, tenantId, Role.ADMIN, true, now);
        when(registerTenantUseCase.register(any()))
                .thenReturn(new RegisterTenantResult("jwt-token", 3600, tenant, admin, membership));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Constructora Nueva\",\"taxId\":\"NIT-123\","
                                + "\"address\":\"Calle 1 # 2-3\",\"adminFullName\":\"Jefe Nueva\","
                                + "\"adminEmail\":\"jefe@nueva.com\",\"adminPassword\":\"Secreta123!\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.tenant.slug").value("constructora-nueva"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.user.email").value("jefe@nueva.com"));
    }

    @Test
    void registroConEmailDuplicadoDevuelve409() throws Exception {
        when(registerTenantUseCase.register(any()))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.CONFLICT, "El email ya está registrado"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Otra\",\"taxId\":\"NIT-123\","
                                + "\"address\":\"Calle 1 # 2-3\",\"adminFullName\":\"Admin\","
                                + "\"adminEmail\":\"admin@demo.com\",\"adminPassword\":\"Secreta123!\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void registroConPasswordCortaDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Otra\",\"taxId\":\"NIT-123\","
                                + "\"address\":\"Calle 1 # 2-3\",\"adminFullName\":\"Admin\","
                                + "\"adminEmail\":\"jefe@nueva.com\",\"adminPassword\":\"corta\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registroConPasswordSinSimboloDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Otra\",\"taxId\":\"NIT-123\","
                                + "\"address\":\"Calle 1 # 2-3\",\"adminFullName\":\"Admin\","
                                + "\"adminEmail\":\"jefe@nueva.com\",\"adminPassword\":\"Secreta123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.adminPassword").value(
                        "La contraseña debe tener al menos 8 caracteres e incluir letras, números y símbolos"));
    }

    @Test
    void registroSinTaxIdDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Otra\",\"adminFullName\":\"Admin\","
                                + "\"taxId\":\"\",\"address\":\"Calle 1 # 2-3\","
                                + "\"adminEmail\":\"jefe@nueva.com\","
                                + "\"adminPassword\":\"Secreta123!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.taxId").exists());
    }

    @Test
    void registroSinDireccionDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Otra\",\"adminFullName\":\"Admin\","
                                + "\"taxId\":\"NIT-123\",\"address\":\"\","
                                + "\"adminEmail\":\"jefe@nueva.com\","
                                + "\"adminPassword\":\"Secreta123!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.address").exists());
    }

    @Test
    void googleLoginDeCuentaExistenteDevuelve200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Tenant tenant = new Tenant(tenantId, "Constructora Demo", "demo", now);
        User user = new User(userId, "ana@gmail.com", "Ana Pérez", "hash", true, now);
        Membership membership = new Membership(
                UUID.randomUUID(), userId, tenantId, Role.ADMIN, true, now);
        when(googleAuthUseCase.authenticate(any()))
                .thenReturn(new GoogleAuthResult("jwt-token", 3600, false, tenant, user, membership));

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"token-google\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.newUser").value(false))
                .andExpect(jsonPath("$.user.email").value("ana@gmail.com"))
                .andExpect(jsonPath("$.tenant.slug").value("demo"));
    }

    @Test
    void googleRegistroDeCuentaNuevaDevuelve201() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Tenant tenant = new Tenant(tenantId, "Constructora Nueva", "constructora-nueva", now);
        User user = new User(userId, "nueva@gmail.com", "Nueva Admin", "hash", true, now);
        Membership membership = new Membership(
                UUID.randomUUID(), userId, tenantId, Role.ADMIN, true, now);
        when(googleAuthUseCase.authenticate(any()))
                .thenReturn(new GoogleAuthResult("jwt-token", 3600, true, tenant, user, membership));

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"token-google\",\"companyName\":\"Constructora Nueva\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.newUser").value(true))
                .andExpect(jsonPath("$.tenant.name").value("Constructora Nueva"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    void googleSinTokenDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void googleConTokenInvalidoDevuelve401() throws Exception {
        when(googleAuthUseCase.authenticate(any()))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED, "Token de Google inválido o expirado"));

        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idToken\":\"token-malo\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void forgotPasswordSiempreDevuelve200() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@demo.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", notNullValue()));
    }

    @Test
    void forgotPasswordConEmailInvalidoDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"no-es-email\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetPasswordValidoDevuelve200() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"token-valido\",\"newPassword\":\"NuevaClave123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", notNullValue()));
    }

    @Test
    void resetPasswordConPasswordCortaDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"token-valido\",\"newPassword\":\"corta\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetPasswordSinSimboloDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"token-valido\",\"newPassword\":\"NuevaClave123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.newPassword").value(
                        "La contraseña debe tener al menos 8 caracteres e incluir letras, números y símbolos"));
    }

    @Test
    void resetPasswordConTokenInvalidoDevuelve400() throws Exception {
        org.mockito.Mockito.doThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Token inválido o expirado"))
                .when(resetPasswordUseCase).reset(any(), any());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"malo\",\"newPassword\":\"NuevaClave123!\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void previewDeInvitacionValidaDevuelveDatosDelTenant() throws Exception {
        when(invitationQueryService.preview("token-valido"))
                .thenReturn(new InvitationPreview("Constructora Demo", "obrero@demo.com",
                        java.time.LocalDateTime.of(2026, 12, 1, 10, 0), true));

        mockMvc.perform(get("/api/auth/invitations/token-valido"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyName").value("Constructora Demo"))
                .andExpect(jsonPath("$.email").value("obrero@demo.com"))
                .andExpect(jsonPath("$.expiresAt").value("2026-12-01T10:00:00"))
                .andExpect(jsonPath("$.requiresPassword").value(true));
    }

    @Test
    void previewDeInvitacionParaCuentaExistenteIndicaQueNoRequierePassword() throws Exception {
        when(invitationQueryService.preview("token-existente"))
                .thenReturn(new InvitationPreview("Constructora Demo", "existente@demo.com",
                        java.time.LocalDateTime.of(2026, 12, 1, 10, 0), false));

        mockMvc.perform(get("/api/auth/invitations/token-existente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiresPassword").value(false));
    }

    @Test
    void previewDeInvitacionInvalidaDevuelve400() throws Exception {
        when(invitationQueryService.preview("token-malo"))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "La invitación no existe, expiró o ya fue utilizada"));

        mockMvc.perform(get("/api/auth/invitations/token-malo"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aceptarInvitacionValidaDevuelve200YSesion() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        User user = new User(userId, "obrero@demo.com", "Obrero Demo", "hash", true, now);
        Membership membership = new Membership(
                UUID.randomUUID(), userId, tenantId, Role.WORKER, true, now);
        when(acceptInvitationUseCase.accept(any()))
                .thenReturn(new AuthenticationResult("jwt-token", 3600, user, membership));

        mockMvc.perform(post("/api/auth/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"token-valido\",\"password\":\"NuevaClave123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.user.email").value("obrero@demo.com"))
                .andExpect(jsonPath("$.user.role").value("WORKER"));
    }

    @Test
    void aceptarInvitacionSinTokenDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aceptarInvitacionConPasswordCortaDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"token-valido\",\"password\":\"corta\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aceptarInvitacionSinSimboloDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"token-valido\",\"password\":\"NuevaClave123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.password").value(
                        "La contraseña debe tener al menos 8 caracteres e incluir letras, números y símbolos"));
    }
}
