package com.SaaS.Constructora.identity.interfaces;

import com.SaaS.Constructora.identity.application.LoginUseCase;
import com.SaaS.Constructora.identity.application.RegisterTenantUseCase;
import com.SaaS.Constructora.identity.domain.Role;
import com.SaaS.Constructora.identity.interfaces.dto.LoginResponse;
import com.SaaS.Constructora.identity.interfaces.dto.RegisterTenantResponse;
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
}
