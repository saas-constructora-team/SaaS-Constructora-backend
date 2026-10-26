package com.saas.constructora.modules.identity.interfaces;

import com.saas.constructora.modules.identity.application.GoogleAuthUseCase;
import com.saas.constructora.modules.identity.application.LoginUseCase;
import com.saas.constructora.modules.identity.application.RegisterTenantUseCase;
import com.saas.constructora.modules.identity.application.RequestPasswordResetUseCase;
import com.saas.constructora.modules.identity.application.ResetPasswordUseCase;
import com.saas.constructora.modules.identity.domain.Role;
import com.saas.constructora.modules.identity.interfaces.dto.GoogleAuthResponse;
import com.saas.constructora.modules.identity.interfaces.dto.LoginResponse;
import com.saas.constructora.modules.identity.interfaces.dto.RegisterTenantResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.notNullValue;
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

    @Test
    void loginConCredenciasValidasDevuelve200YToken() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        LoginResponse response = new LoginResponse("jwt-token", "Bearer", 3600,
                new LoginResponse.UserResponse(userId, tenantId, "admin@demo.com",
                        "Administrador Demo", Role.ADMIN.name()));
        when(loginUseCase.login(any())).thenReturn(response);

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
        RegisterTenantResponse response = new RegisterTenantResponse("jwt-token", "Bearer", 3600,
                new RegisterTenantResponse.TenantResponse(tenantId, "Constructora Nueva", "constructora-nueva"),
                new LoginResponse.UserResponse(userId, tenantId, "jefe@nueva.com",
                        "Jefe Nueva", Role.ADMIN.name()));
        when(registerTenantUseCase.register(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Constructora Nueva\",\"adminFullName\":\"Jefe Nueva\","
                                + "\"adminEmail\":\"jefe@nueva.com\",\"adminPassword\":\"Secreta123\"}"))
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
                        .content("{\"companyName\":\"Otra\",\"adminFullName\":\"Admin\","
                                + "\"adminEmail\":\"admin@demo.com\",\"adminPassword\":\"Secreta123\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void registroConPasswordCortaDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Otra\",\"adminFullName\":\"Admin\","
                                + "\"adminEmail\":\"jefe@nueva.com\",\"adminPassword\":\"corta\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void googleLoginDeCuentaExistenteDevuelve200() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        GoogleAuthResponse response = new GoogleAuthResponse("jwt-token", "Bearer", 3600, false,
                new GoogleAuthResponse.TenantResponse(tenantId, "Constructora Demo", "demo"),
                new LoginResponse.UserResponse(userId, tenantId, "ana@gmail.com",
                        "Ana Pérez", Role.ADMIN.name()));
        when(googleAuthUseCase.authenticate(any())).thenReturn(response);

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
        GoogleAuthResponse response = new GoogleAuthResponse("jwt-token", "Bearer", 3600, true,
                new GoogleAuthResponse.TenantResponse(tenantId, "Constructora Nueva", "constructora-nueva"),
                new LoginResponse.UserResponse(userId, tenantId, "nueva@gmail.com",
                        "Nueva Admin", Role.ADMIN.name()));
        when(googleAuthUseCase.authenticate(any())).thenReturn(response);

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
                        .content("{\"token\":\"token-valido\",\"newPassword\":\"NuevaClave123\"}"))
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
    void resetPasswordConTokenInvalidoDevuelve400() throws Exception {
        org.mockito.Mockito.doThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Token inválido o expirado"))
                .when(resetPasswordUseCase).reset(any(), any());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"malo\",\"newPassword\":\"NuevaClave123\"}"))
                .andExpect(status().isBadRequest());
    }
}
