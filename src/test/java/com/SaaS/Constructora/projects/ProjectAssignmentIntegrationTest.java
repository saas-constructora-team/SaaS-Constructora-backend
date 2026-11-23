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
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración de asignaciones y visibilidad de proyectos:
 * dos empresas, usuarios MEMBER y ADMIN, y PostgreSQL real con Flyway.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ProjectAssignmentIntegrationTest {

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
    void adminListaLosUsuariosDeSuTenant() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        String memberEmail = uniqueEmail("miembro");
        createMember(DEMO_TENANT, memberEmail, "Miembro Demo");

        String otherEmail = uniqueEmail("otro-admin");
        Session other = registerCompanyAndGetSession(otherEmail);

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].email", hasItem(memberEmail)))
                .andExpect(jsonPath("$[*].email", not(hasItem(other.email()))));
    }

    @Test
    void memberNoPuedeListarUsuarios() throws Exception {
        Session member = createMemberSession(DEMO_TENANT, uniqueEmail("miembro"));

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + member.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminAsignaUnMiembroYLoLista() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        Session member = createMemberSession(DEMO_TENANT, uniqueEmail("miembro"));
        UUID projectId = createProject(demo.token(), "Proyecto asignable " + UUID.randomUUID());

        mockMvc.perform(post("/api/projects/{projectId}/assignments", projectId)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + member.userId() + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").value(projectId.toString()))
                .andExpect(jsonPath("$.userId").value(member.userId().toString()))
                .andExpect(jsonPath("$.userEmail").value(member.email()))
                .andExpect(jsonPath("$.userRole").value("MEMBER"));

        mockMvc.perform(get("/api/projects/{projectId}/assignments", projectId)
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].userId", hasItem(member.userId().toString())));
    }

    @Test
    void noSePuedeAsignarDosVecesAlMismoUsuario() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        Session member = createMemberSession(DEMO_TENANT, uniqueEmail("miembro"));
        UUID projectId = createProject(demo.token(), "Proyecto duplicado " + UUID.randomUUID());
        assign(demo.token(), projectId, member.userId());

        mockMvc.perform(post("/api/projects/{projectId}/assignments", projectId)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + member.userId() + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void memberNoPuedeAsignarProyectos() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        Session member = createMemberSession(DEMO_TENANT, uniqueEmail("miembro"));
        UUID projectId = createProject(demo.token(), "Proyecto protegido " + UUID.randomUUID());

        mockMvc.perform(post("/api/projects/{projectId}/assignments", projectId)
                        .header("Authorization", "Bearer " + member.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + member.userId() + "\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/projects/{projectId}/assignments/{userId}",
                        projectId, member.userId())
                        .header("Authorization", "Bearer " + member.token()))
                .andExpect(status().isForbidden());
    }

    @Test
    void noSeAsignaUnProyectoDeOtraEmpresa() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        Session member = createMemberSession(DEMO_TENANT, uniqueEmail("miembro"));
        Session other = registerCompanyAndGetSession(uniqueEmail("otro-admin"));
        UUID otherProject = createProject(other.token(), "Proyecto ajeno " + UUID.randomUUID());

        mockMvc.perform(post("/api/projects/{projectId}/assignments", otherProject)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + member.userId() + "\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void noSeAsignaUnUsuarioDeOtraEmpresa() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        Session other = registerCompanyAndGetSession(uniqueEmail("otro-admin"));
        UUID projectId = createProject(demo.token(), "Proyecto propio " + UUID.randomUUID());

        mockMvc.perform(post("/api/projects/{projectId}/assignments", projectId)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + other.userId() + "\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void memberSoloVeLosProyectosQueTieneAsignados() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        Session member = createMemberSession(DEMO_TENANT, uniqueEmail("miembro"));
        String assignedName = "Proyecto asignado " + UUID.randomUUID();
        String otherName = "Proyecto no asignado " + UUID.randomUUID();
        UUID assignedProject = createProject(demo.token(), assignedName);
        createProject(demo.token(), otherName);
        assign(demo.token(), assignedProject, member.userId());

        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + member.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem(assignedName)))
                .andExpect(jsonPath("$[*].name", not(hasItem(otherName))));
    }

    @Test
    void adminSigueViendoTodosLosProyectos() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        String name = "Proyecto visible admin " + UUID.randomUUID();
        createProject(demo.token(), name);

        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem(name)));
    }

    @Test
    void desasignarQuitaElProyectoDeLaListaDelMember() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        Session member = createMemberSession(DEMO_TENANT, uniqueEmail("miembro"));
        String name = "Proyecto desasignable " + UUID.randomUUID();
        UUID projectId = createProject(demo.token(), name);
        assign(demo.token(), projectId, member.userId());

        mockMvc.perform(delete("/api/projects/{projectId}/assignments/{userId}",
                        projectId, member.userId())
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects/{projectId}/assignments", projectId)
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].userId", not(hasItem(member.userId().toString()))));

        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + member.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", not(hasItem(name))));
    }

    @Test
    void desasignarConservaElHistorialYPermiteReasignar() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        Session member = createMemberSession(DEMO_TENANT, uniqueEmail("miembro"));
        UUID projectId = createProject(demo.token(), "Proyecto reasignable " + UUID.randomUUID());
        assign(demo.token(), projectId, member.userId());

        mockMvc.perform(delete("/api/projects/{projectId}/assignments/{userId}",
                        projectId, member.userId())
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isNoContent());

        Integer activas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM project_assignments WHERE project_id = ? "
                        + "AND unassigned_at IS NULL", Integer.class, projectId);
        Integer historial = jdbc.queryForObject(
                "SELECT COUNT(*) FROM project_assignments WHERE project_id = ?",
                Integer.class, projectId);
        assertEquals(0, activas);
        assertEquals(1, historial);

        assign(demo.token(), projectId, member.userId());
    }

    @Test
    void eliminarElProyectoBorraSusAsignaciones() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        Session member = createMemberSession(DEMO_TENANT, uniqueEmail("miembro"));
        UUID projectId = createProject(demo.token(), "Proyecto borrable " + UUID.randomUUID());
        assign(demo.token(), projectId, member.userId());

        mockMvc.perform(delete("/api/projects/{id}", projectId)
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isNoContent());

        Integer remaining = jdbc.queryForObject(
                "SELECT COUNT(*) FROM project_assignments WHERE project_id = ?",
                Integer.class, projectId);
        assertEquals(0, remaining);
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/projects/{projectId}/assignments", UUID.randomUUID()))
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
        createMember(tenantId, email, "Miembro " + email);
        return login(email, MEMBER_PASSWORD);
    }

    private void createMember(UUID tenantId, String email, String fullName) {
        UUID userId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO users (id, email, full_name, password_hash, active)
                VALUES (?, ?, ?, ?, TRUE)
                """,
                userId, email, fullName,
                passwordEncoder.encode(MEMBER_PASSWORD));
        jdbc.update("""
                INSERT INTO memberships (id, user_id, tenant_id, role, active)
                VALUES (?, ?, ?, 'MEMBER', TRUE)
                """,
                UUID.randomUUID(), userId, tenantId);
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

    private static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@prueba.com";
    }

    private record Session(String token, UUID userId, String email, UUID tenantId) { }
}
