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

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de autorización por rol y aislamiento de tenant en las operaciones
 * de proyectos: crear (solo ADMIN), editar (ADMIN o MEMBER asignado) y
 * eliminar (solo ADMIN).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ProjectAuthorizationIntegrationTest {

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
    void memberNoPuedeCrearProyectos() throws Exception {
        Session member = createMemberSession(DEMO_TENANT, uniqueEmail("miembro"));

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + member.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectBody("Proyecto del member " + UUID.randomUUID())))
                .andExpect(status().isForbidden());
    }

    @Test
    void memberAsignadoPuedeEditarElProyecto() throws Exception {
        Session admin = login("admin@demo.com", "Admin123!");
        Session member = createMemberSession(DEMO_TENANT, uniqueEmail("miembro"));
        UUID projectId = createProject(admin.token(), "Proyecto editable " + UUID.randomUUID());
        assign(admin.token(), projectId, member.userId());

        String updatedName = "Proyecto editado por member " + UUID.randomUUID();

        mockMvc.perform(put("/api/projects/{id}", projectId)
                        .header("Authorization", "Bearer " + member.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectBody(updatedName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(projectId.toString()))
                .andExpect(jsonPath("$.tenantId").value(DEMO_TENANT.toString()))
                .andExpect(jsonPath("$.name").value(updatedName));
    }

    @Test
    void memberNoAsignadoNoPuedeEditarElProyecto() throws Exception {
        Session admin = login("admin@demo.com", "Admin123!");
        Session member = createMemberSession(DEMO_TENANT, uniqueEmail("miembro"));
        String originalName = "Proyecto no asignado " + UUID.randomUUID();
        UUID projectId = createProject(admin.token(), originalName);

        mockMvc.perform(put("/api/projects/{id}", projectId)
                        .header("Authorization", "Bearer " + member.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectBody(originalName + " editado")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem(originalName)));
    }

    @Test
    void memberNoPuedeEliminarProyectosAunqueEsteAsignado() throws Exception {
        Session admin = login("admin@demo.com", "Admin123!");
        Session member = createMemberSession(DEMO_TENANT, uniqueEmail("miembro"));
        String name = "Proyecto protegido " + UUID.randomUUID();
        UUID projectId = createProject(admin.token(), name);
        assign(admin.token(), projectId, member.userId());

        mockMvc.perform(delete("/api/projects/{id}", projectId)
                        .header("Authorization", "Bearer " + member.token()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem(name)));
    }

    @Test
    void memberDeOtraEmpresaNoPuedeEditarNiEliminarElProyecto() throws Exception {
        Session admin = login("admin@demo.com", "Admin123!");
        Session other = registerCompanyAndGetSession(uniqueEmail("otro-admin"));
        String name = "Proyecto ajeno " + UUID.randomUUID();
        UUID projectId = createProject(admin.token(), name);

        mockMvc.perform(put("/api/projects/{id}", projectId)
                        .header("Authorization", "Bearer " + other.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectBody(name + " editado")))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/projects/{id}", projectId)
                        .header("Authorization", "Bearer " + other.token()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem(name)));
    }

    @Test
    void endpointNoDeclaradoExigeAutenticacion() throws Exception {
        mockMvc.perform(get("/api/ruta-inexistente"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sinTokenDevuelve401EnLasOperacionesDeProyecto() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectBody("Proyecto " + UUID.randomUUID())))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/projects/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectBody("Proyecto " + UUID.randomUUID())))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/projects/{id}", UUID.randomUUID()))
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
                JsonPath.read(body, "$.user.email"),
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
        return new Session(
                JsonPath.read(body, "$.accessToken"),
                UUID.fromString(JsonPath.read(body, "$.user.id")),
                JsonPath.read(body, "$.user.email"),
                UUID.fromString(JsonPath.read(body, "$.tenant.id")));
    }

    private Session createMemberSession(UUID tenantId, String email) throws Exception {
        UUID userId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO users (id, email, full_name, password_hash, active)
                VALUES (?, ?, ?, ?, TRUE)
                """,
                userId, email, "Miembro " + email,
                passwordEncoder.encode(MEMBER_PASSWORD));
        jdbc.update("""
                INSERT INTO memberships (id, user_id, tenant_id, role, active)
                VALUES (?, ?, ?, 'MEMBER', TRUE)
                """,
                UUID.randomUUID(), userId, tenantId);
        return login(email, MEMBER_PASSWORD);
    }

    private UUID createProject(String token, String name) throws Exception {
        String body = mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectBody(name)))
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

    private static String projectBody(String name) {
        return "{\"name\":\"" + name + "\",\"clientName\":\"Inmobiliaria XYZ\","
                + "\"location\":\"Tuluá, Valle del Cauca\"}";
    }

    private static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@prueba.com";
    }

    private record Session(String token, UUID userId, String email, UUID tenantId) { }
}
