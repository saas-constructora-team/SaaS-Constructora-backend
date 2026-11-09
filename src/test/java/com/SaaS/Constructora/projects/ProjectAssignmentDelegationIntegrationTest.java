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
 * Delegación de la gestión de un proyecto: un ADMIN nombra LEAD a un MEMBER
 * y ese LEAD puede administrar a los demás miembros, sin poder nombrar LEADs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ProjectAssignmentDelegationIntegrationTest {

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
    void adminDelegaLeadYElLeadGestionaMiembros() throws Exception {
        Session admin = login("admin@demo.com", "Admin123!");
        Session lead = createMemberSession(DEMO_TENANT, uniqueEmail("lead"));
        Session member = createMemberSession(DEMO_TENANT, uniqueEmail("miembro"));
        UUID projectId = createProject(admin.token(), "Proyecto delegado " + UUID.randomUUID());

        // El ADMIN nombra LEAD al usuario dentro del proyecto.
        mockMvc.perform(post("/api/projects/{projectId}/assignments", projectId)
                        .header("Authorization", "Bearer " + admin.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + lead.userId() + "\",\"role\":\"LEAD\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectRole").value("LEAD"))
                .andExpect(jsonPath("$.userRole").value("MEMBER"));

        // El LEAD puede listar y agregar MEMBERs.
        mockMvc.perform(get("/api/projects/{projectId}/assignments", projectId)
                        .header("Authorization", "Bearer " + lead.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].projectRole", hasItem("LEAD")));

        mockMvc.perform(post("/api/projects/{projectId}/assignments", projectId)
                        .header("Authorization", "Bearer " + lead.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + member.userId() + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectRole").value("MEMBER"));

        // El LEAD no puede promover a otro LEAD.
        mockMvc.perform(put("/api/projects/{projectId}/assignments/{userId}",
                        projectId, member.userId())
                        .header("Authorization", "Bearer " + lead.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"LEAD\"}"))
                .andExpect(status().isForbidden());

        // El LEAD sí puede quitar a un MEMBER.
        mockMvc.perform(delete("/api/projects/{projectId}/assignments/{userId}",
                        projectId, member.userId())
                        .header("Authorization", "Bearer " + lead.token()))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminCambiaElRolDeUnMiembroDelProyecto() throws Exception {
        Session admin = login("admin@demo.com", "Admin123!");
        Session member = createMemberSession(DEMO_TENANT, uniqueEmail("miembro"));
        UUID projectId = createProject(admin.token(), "Proyecto editable " + UUID.randomUUID());

        assign(admin.token(), projectId, member.userId());

        mockMvc.perform(put("/api/projects/{projectId}/assignments/{userId}",
                        projectId, member.userId())
                        .header("Authorization", "Bearer " + admin.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"LEAD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectRole").value("LEAD"))
                .andExpect(jsonPath("$.userId").value(member.userId().toString()));
    }

    @Test
    void memberLeadDeOtroProyectoNoPuedeGestionarEste() throws Exception {
        Session admin = login("admin@demo.com", "Admin123!");
        Session lead = createMemberSession(DEMO_TENANT, uniqueEmail("lead"));
        Session member = createMemberSession(DEMO_TENANT, uniqueEmail("miembro"));
        UUID leadProject = createProject(admin.token(), "Proyecto del lead " + UUID.randomUUID());
        UUID otherProject = createProject(admin.token(), "Proyecto ajeno " + UUID.randomUUID());
        assignAsLead(admin.token(), leadProject, lead.userId());

        mockMvc.perform(post("/api/projects/{projectId}/assignments", otherProject)
                        .header("Authorization", "Bearer " + lead.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + member.userId() + "\"}"))
                .andExpect(status().isForbidden());
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

    private void assignAsLead(String token, UUID projectId, UUID userId) throws Exception {
        mockMvc.perform(post("/api/projects/{projectId}/assignments", projectId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + userId + "\",\"role\":\"LEAD\"}"))
                .andExpect(status().isCreated());
    }

    private static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@prueba.com";
    }

    private record Session(String token, UUID userId, String email, UUID tenantId) { }
}
