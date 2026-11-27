package com.SaaS.Constructora.projects;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La etapa en curso es estado del proyecto, no del navegador: cualquiera con
 * acceso al proyecto debe ver la misma selección.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ProjectProgressIntegrationTest {

    private static final UUID DEMO_TENANT =
            UUID.fromString("5f2b9c1d-3e47-4a86-9c21-8d4f0a7b5e13");
    private static final String MEMBER_PASSWORD = "Member123!";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void adminCambiaLaEtapaYElMiembroAsignadoVeElMismoAvance() throws Exception {
        Session admin = login("admin@demo.com", "Admin123!");
        Session member = createMemberSession(DEMO_TENANT, uniqueEmail("worker"));
        UUID projectId = createProject(admin.token(), "Proyecto avance " + UUID.randomUUID());
        assign(admin.token(), projectId, member.userId());
        String stages = stages(admin.token(), projectId);
        UUID secondStage = UUID.fromString(JsonPath.read(stages, "$[1].id"));

        mockMvc.perform(put("/api/projects/{projectId}/progress", projectId)
                        .header("Authorization", "Bearer " + admin.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stageId\":\"" + secondStage + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(projectId.toString()))
                .andExpect(jsonPath("$.currentStageId").value(secondStage.toString()))
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(get("/api/projects/{projectId}/progress", projectId)
                        .header("Authorization", "Bearer " + member.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStageId").value(secondStage.toString()));

        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + member.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].currentStageId").value(secondStage.toString()));
    }

    @Test
    void otraEmpresaNoPuedeLeerNiCambiarElAvance() throws Exception {
        Session admin = login("admin@demo.com", "Admin123!");
        Session other = registerCompanyAndGetSession(uniqueEmail("otro-admin"));
        UUID projectId = createProject(admin.token(), "Proyecto avance ajeno " + UUID.randomUUID());
        UUID stage = UUID.fromString(JsonPath.read(stages(admin.token(), projectId), "$[0].id"));

        mockMvc.perform(get("/api/projects/{projectId}/progress", projectId)
                        .header("Authorization", "Bearer " + other.token()))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/projects/{projectId}/progress", projectId)
                        .header("Authorization", "Bearer " + other.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stageId\":\"" + stage + "\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void miembroNoAsignadoNoPuedeLeerNiCambiarElAvance() throws Exception {
        Session admin = login("admin@demo.com", "Admin123!");
        Session member = createMemberSession(DEMO_TENANT, uniqueEmail("miembro"));
        UUID projectId = createProject(admin.token(), "Proyecto avance privado " + UUID.randomUUID());
        UUID stage = UUID.fromString(JsonPath.read(stages(admin.token(), projectId), "$[0].id"));

        mockMvc.perform(get("/api/projects/{projectId}/progress", projectId)
                        .header("Authorization", "Bearer " + member.token()))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/projects/{projectId}/progress", projectId)
                        .header("Authorization", "Bearer " + member.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stageId\":\"" + stage + "\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void unaEtapaCerradaNoPuedeSeleccionarseComoActual() throws Exception {
        Session admin = login("admin@demo.com", "Admin123!");
        UUID projectId = createProject(admin.token(), "Proyecto etapa cerrada " + UUID.randomUUID());
        UUID stage = UUID.fromString(JsonPath.read(stages(admin.token(), projectId), "$[0].id"));

        mockMvc.perform(post("/api/projects/{projectId}/stages/{stageId}/close", projectId, stage)
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/projects/{projectId}/progress", projectId)
                        .header("Authorization", "Bearer " + admin.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stageId\":\"" + stage + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void eliminarLaEtapaEnCursoDejaElProyectoSinEtapaSeleccionada() throws Exception {
        Session admin = login("admin@demo.com", "Admin123!");
        UUID projectId = createProject(admin.token(), "Proyecto borrar avance " + UUID.randomUUID());
        UUID stage = UUID.fromString(JsonPath.read(stages(admin.token(), projectId), "$[1].id"));

        mockMvc.perform(put("/api/projects/{projectId}/progress", projectId)
                        .header("Authorization", "Bearer " + admin.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stageId\":\"" + stage + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/projects/{projectId}/stages/{stageId}", projectId, stage)
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isNoContent());

        String body = mockMvc.perform(get("/api/projects/{projectId}/progress", projectId)
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertNull(JsonPath.read(body, "$.currentStageId"));
    }

    @Test
    void elCanalSseSoloAceptaProyectosVisiblesDelTenant() throws Exception {
        Session admin = login("admin@demo.com", "Admin123!");
        Session other = registerCompanyAndGetSession(uniqueEmail("otro-sse"));
        Session member = createMemberSession(DEMO_TENANT, uniqueEmail("sse-member"));
        UUID projectId = createProject(admin.token(), "Proyecto sse " + UUID.randomUUID());

        mockMvc.perform(get("/api/projects/events").param("projectId", projectId.toString())
                        .header("Authorization", "Bearer " + other.token()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/projects/events").param("projectId", projectId.toString())
                        .header("Authorization", "Bearer " + member.token()))
                .andExpect(status().isNotFound());
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}/progress", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/projects/events"))
                .andExpect(status().isUnauthorized());
    }

    private Session login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return new Session(
                JsonPath.read(body, "$.accessToken"),
                UUID.fromString(JsonPath.read(body, "$.user.id")),
                UUID.fromString(JsonPath.read(body, "$.user.tenantId")));
    }

    private Session registerCompanyAndGetSession(String adminEmail) throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Constructora " + suffix + "\","
                                + "\"taxId\":\"NIT-" + suffix + "\","
                                + "\"address\":\"Calle Prueba 123\","
                                + "\"adminFullName\":\"Admin Prueba\","
                                + "\"adminEmail\":\"" + adminEmail + "\","
                                + "\"adminPassword\":\"Secreta123!\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new Session(JsonPath.read(body, "$.accessToken"),
                UUID.fromString(JsonPath.read(body, "$.user.id")),
                UUID.fromString(JsonPath.read(body, "$.tenant.id")));
    }

    private Session createMemberSession(UUID tenantId, String email) throws Exception {
        UUID userId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO users (id, email, full_name, password_hash, active)
                VALUES (?, ?, ?, ?, TRUE)
                """, userId, email, "Miembro " + email, passwordEncoder.encode(MEMBER_PASSWORD));
        jdbc.update("""
                INSERT INTO memberships (id, user_id, tenant_id, role, active)
                VALUES (?, ?, ?, 'MEMBER', TRUE)
                """, UUID.randomUUID(), userId, tenantId);
        return login(email, MEMBER_PASSWORD);
    }

    private UUID createProject(String token, String name) throws Exception {
        String body = mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"clientName\":\"Inmobiliaria XYZ\","
                                + "\"location\":\"Tuluá, Valle del Cauca\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
    }

    private void assign(String token, UUID projectId, UUID userId) throws Exception {
        mockMvc.perform(post("/api/projects/{projectId}/assignments", projectId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + userId + "\"}"))
                .andExpect(status().isCreated());
    }

    private String stages(String token, UUID projectId) throws Exception {
        return mockMvc.perform(get("/api/projects/{projectId}/stages", projectId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@prueba.com";
    }

    private record Session(String token, UUID userId, UUID tenantId) { }
}
