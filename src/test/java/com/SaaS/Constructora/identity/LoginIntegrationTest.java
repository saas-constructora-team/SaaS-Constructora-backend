package com.SaaS.Constructora.identity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración de la rebanada HU-01: arranca PostgreSQL real (Testcontainers),
 * ejecuta todas las migraciones de Flyway (incluida V3__create_identity) y ejercita
 * el endpoint de login con el usuario inicial sembrado por la migración.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class LoginIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginConUsuarioSeedDevuelve200YToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@demo.com\",\"password\":\"Admin123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600))
                .andExpect(jsonPath("$.user.email").value("admin@demo.com"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.user.tenantId").value("5f2b9c1d-3e47-4a86-9c21-8d4f0a7b5e13"));
    }

    @Test
    void loginConPasswordIncorrectaDevuelve401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@demo.com\",\"password\":\"otra\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginConEmailInexistenteDevuelve401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@demo.com\",\"password\":\"Admin123!\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginSinBodyDevuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registroDeEmpresaYPosteriorLoginFuncionan() throws Exception {
        // Registro abierto: cualquiera crea su empresa y queda con sesión iniciada
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Constructora Andina\",\"adminFullName\":\"Ana Pérez\","
                                + "\"adminEmail\":\"ana@andina.com\",\"adminPassword\":\"Secreta123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.tenant.name").value("Constructora Andina"))
                .andExpect(jsonPath("$.tenant.slug").value("constructora-andina"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.user.email").value("ana@andina.com"));

        // El admin recién creado puede autenticarse con sus credenciales
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@andina.com\",\"password\":\"Secreta123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.user.role").value("ADMIN"));
    }

    @Test
    void registroConEmailYaUsadoDevuelve409() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Otra Empresa\",\"adminFullName\":\"Otro\","
                                + "\"adminEmail\":\"admin@demo.com\",\"adminPassword\":\"Secreta123\"}"))
                .andExpect(status().isConflict());
    }
}
