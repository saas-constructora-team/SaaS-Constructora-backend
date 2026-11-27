package com.SaaS.Constructora.sitelog;

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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Recorrido HTTP real de la bitácora: CRUD, tenant y permiso de edición. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SiteLogRecordIntegrationTest {

    private static final UUID DEMO_TENANT =
            UUID.fromString("5f2b9c1d-3e47-4a86-9c21-8d4f0a7b5e13");
    private static final String WORKER_PASSWORD = "Worker123!";

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
    void adminCreaListaEditaYEliminaUnaEntrada() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        UUID projectId = createProject(demo.token());

        String created = mockMvc.perform(post("/api/projects/{projectId}/site-log", projectId)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entryBody("AVANCE", "ABIERTO", "Avance de obra")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").value(projectId.toString()))
                .andExpect(jsonPath("$.type").value("AVANCE"))
                .andExpect(jsonPath("$.status").value("ABIERTO"))
                .andExpect(jsonPath("$.what").value("Avance de obra"))
                .andExpect(jsonPath("$.author").exists())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.photos").value(0))
                .andReturn().getResponse().getContentAsString();
        UUID entryId = UUID.fromString(JsonPath.read(created, "$.id"));

        mockMvc.perform(get("/api/projects/{projectId}/site-log", projectId)
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(entryId.toString()));

        mockMvc.perform(put("/api/projects/{projectId}/site-log/{entryId}", projectId, entryId)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entryBody("AVANCE", "RESUELTO", "Avance terminado")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESUELTO"));

        mockMvc.perform(patch("/api/projects/{projectId}/site-log/{entryId}/touch",
                        projectId, entryId)
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

        mockMvc.perform(delete("/api/projects/{projectId}/site-log/{entryId}",
                        projectId, entryId)
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects/{projectId}/site-log", projectId)
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void campoVacioDevuelve400() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        UUID projectId = createProject(demo.token());

        mockMvc.perform(post("/api/projects/{projectId}/site-log", projectId)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entryBody("AVANCE", "ABIERTO", "   ")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void otroTenantNoPuedeCrearNiConsultar() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        UUID projectId = createProject(demo.token());
        Session other = registerCompanyAndGetSession();

        mockMvc.perform(post("/api/projects/{projectId}/site-log", projectId)
                        .header("Authorization", "Bearer " + other.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entryBody("AVANCE", "ABIERTO", "Avance ajeno")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/projects/{projectId}/site-log", projectId)
                        .header("Authorization", "Bearer " + other.token()))
                .andExpect(status().isNotFound());
    }

    @Test
    void workerSoloEditaCuandoElAdminLeConcedeElPermiso() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        UUID projectId = createProject(demo.token());
        String created = mockMvc.perform(post("/api/projects/{projectId}/site-log", projectId)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entryBody("NOVEDAD", "ABIERTO", "Novedad del administrador")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID entryId = UUID.fromString(JsonPath.read(created, "$.id"));

        Session worker = createWorkerSession(DEMO_TENANT, uniqueEmail("worker-bitacora"));
        assign(demo.token(), projectId, worker.userId());

        mockMvc.perform(put("/api/projects/{projectId}/site-log/{entryId}", projectId, entryId)
                        .header("Authorization", "Bearer " + worker.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entryBody("NOVEDAD", "RESUELTO", "Editado por trabajador")))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/users/{userId}/site-log-management/enable", worker.userId())
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canManageSiteLog").value(true));

        mockMvc.perform(put("/api/projects/{projectId}/site-log/{entryId}", projectId, entryId)
                        .header("Authorization", "Bearer " + worker.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(entryBody("NOVEDAD", "RESUELTO", "Editado por trabajador")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.what").value("Editado por trabajador"));
    }

    private String entryBody(String type, String status, String what) {
        return "{\"type\":\"" + type + "\",\"status\":\"" + status + "\","
                + "\"when\":\"2026-11-27T10:30:00\",\"where\":\"Piso 3 - Torre A\","
                + "\"what\":\"" + what + "\",\"how\":\"Se registró la evidencia\","
                + "\"why\":\"Clima lluvioso\",\"assigneeId\":null}";
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
                JsonPath.read(body, "$.user.email"),
                UUID.fromString(JsonPath.read(body, "$.user.tenantId")));
    }

    private Session registerCompanyAndGetSession() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Constructora " + suffix + "\","
                                + "\"taxId\":\"NIT-" + suffix + "\","
                                + "\"address\":\"Calle Prueba 123\","
                                + "\"adminFullName\":\"Admin Prueba\","
                                + "\"adminEmail\":\"admin-" + suffix + "@prueba.com\","
                                + "\"adminPassword\":\"Secreta123!\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new Session(
                JsonPath.read(body, "$.accessToken"),
                UUID.fromString(JsonPath.read(body, "$.user.id")),
                JsonPath.read(body, "$.user.email"),
                UUID.fromString(JsonPath.read(body, "$.tenant.id")));
    }

    private Session createWorkerSession(UUID tenantId, String email) throws Exception {
        UUID userId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO users (id, email, full_name, password_hash, active)
                VALUES (?, ?, ?, ?, TRUE)
                """, userId, email, "Trabajador " + email, passwordEncoder.encode(WORKER_PASSWORD));
        jdbc.update("""
                INSERT INTO memberships (id, user_id, tenant_id, role, active)
                VALUES (?, ?, ?, 'WORKER', TRUE)
                """, UUID.randomUUID(), userId, tenantId);
        return login(email, WORKER_PASSWORD);
    }

    private UUID createProject(String token) throws Exception {
        String body = mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Obra bitácora " + UUID.randomUUID() + "\","
                                + "\"clientName\":\"Inmobiliaria XYZ\",\"location\":\"Tuluá\"}"))
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

    private static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@prueba.com";
    }

    private record Session(String token, UUID userId, String email, UUID tenantId) { }
}
