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

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración de etapas: etapas iniciales, CRUD, permisos por rol,
 * asignación a proyectos y aislamiento entre empresas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ProjectStageIntegrationTest {

    private static final UUID DEMO_TENANT =
            UUID.fromString("5f2b9c1d-3e47-4a86-9c21-8d4f0a7b5e13");
    private static final String MEMBER_PASSWORD = "Member123!";
    private static final List<String> INITIAL_STAGES = List.of(
            "Estudios Preliminares y Diagnóstico",
            "Anteproyecto",
            "Proyecto Ejecutivo",
            "Licencias, Permisos y Presupuesto Final",
            "Construcción",
            "Cierre, Liquidación y Entrega");

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
    void proyectoNuevoTieneLasSeisEtapasInicialesEnOrden() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        UUID projectId = createProject(demo.token(), "Proyecto etapas " + UUID.randomUUID());

        mockMvc.perform(get("/api/projects/{projectId}/stages", projectId)
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[0].name").value(INITIAL_STAGES.get(0)))
                .andExpect(jsonPath("$[0].order").value(1))
                .andExpect(jsonPath("$[1].name").value(INITIAL_STAGES.get(1)))
                .andExpect(jsonPath("$[2].name").value(INITIAL_STAGES.get(2)))
                .andExpect(jsonPath("$[3].name").value(INITIAL_STAGES.get(3)))
                .andExpect(jsonPath("$[4].name").value(INITIAL_STAGES.get(4)))
                .andExpect(jsonPath("$[5].name").value(INITIAL_STAGES.get(5)))
                .andExpect(jsonPath("$[5].order").value(6));
    }

    @Test
    void adminAgregaUnaEtapaAlFinal() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        UUID projectId = createProject(demo.token(), "Proyecto etapa extra " + UUID.randomUUID());

        mockMvc.perform(post("/api/projects/{projectId}/stages", projectId)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Etapa adicional\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").value(projectId.toString()))
                .andExpect(jsonPath("$.name").value("Etapa adicional"))
                .andExpect(jsonPath("$.order").value(7));

        mockMvc.perform(get("/api/projects/{projectId}/stages", projectId)
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7)))
                .andExpect(jsonPath("$[6].name").value("Etapa adicional"))
                .andExpect(jsonPath("$[6].order").value(7));
    }

    @Test
    void adminEditaElNombreYConservaElOrden() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        UUID projectId = createProject(demo.token(), "Proyecto editar etapa " + UUID.randomUUID());
        StageRef stage = firstStage(demo.token(), projectId);

        mockMvc.perform(put("/api/projects/{projectId}/stages/{stageId}", projectId, stage.id())
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Diagnóstico actualizado\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(stage.id().toString()))
                .andExpect(jsonPath("$.name").value("Diagnóstico actualizado"))
                .andExpect(jsonPath("$.order").value(stage.order()));

        mockMvc.perform(get("/api/projects/{projectId}/stages", projectId)
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Diagnóstico actualizado"))
                .andExpect(jsonPath("$[0].order").value(stage.order()));
    }

    @Test
    void adminEliminaUnaEtapaYNoReaparece() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        UUID projectId = createProject(demo.token(), "Proyecto borrar etapa " + UUID.randomUUID());
        StageRef stage = firstStage(demo.token(), projectId);

        mockMvc.perform(delete("/api/projects/{projectId}/stages/{stageId}",
                        projectId, stage.id())
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects/{projectId}/stages", projectId)
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[*].name", not(hasItem(INITIAL_STAGES.get(0)))));
    }

    @Test
    void memberAsignadoPuedeGestionarLasEtapas() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        Session member = createMemberSession(DEMO_TENANT, uniqueEmail("miembro"));
        UUID projectId = createProject(demo.token(), "Proyecto member etapas " + UUID.randomUUID());
        assign(demo.token(), projectId, member.userId());

        String createBody = mockMvc.perform(post("/api/projects/{projectId}/stages", projectId)
                        .header("Authorization", "Bearer " + member.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Etapa del miembro\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.order").value(7))
                .andReturn().getResponse().getContentAsString();
        UUID stageId = UUID.fromString(JsonPath.read(createBody, "$.id"));

        mockMvc.perform(put("/api/projects/{projectId}/stages/{stageId}", projectId, stageId)
                        .header("Authorization", "Bearer " + member.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Etapa editada por miembro\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Etapa editada por miembro"))
                .andExpect(jsonPath("$.order").value(7));

        mockMvc.perform(delete("/api/projects/{projectId}/stages/{stageId}", projectId, stageId)
                        .header("Authorization", "Bearer " + member.token()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects/{projectId}/stages", projectId)
                        .header("Authorization", "Bearer " + member.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[*].name", not(hasItem("Etapa editada por miembro"))));
    }

    @Test
    void memberNoAsignadoNoPuedeVerNiGestionarEtapas() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        Session member = createMemberSession(DEMO_TENANT, uniqueEmail("miembro"));
        UUID projectId = createProject(demo.token(), "Proyecto ajeno member " + UUID.randomUUID());
        StageRef stage = firstStage(demo.token(), projectId);

        mockMvc.perform(get("/api/projects/{projectId}/stages", projectId)
                        .header("Authorization", "Bearer " + member.token()))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/projects/{projectId}/stages", projectId)
                        .header("Authorization", "Bearer " + member.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"No permitido\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/projects/{projectId}/stages/{stageId}", projectId, stage.id())
                        .header("Authorization", "Bearer " + member.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"No permitido\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/projects/{projectId}/stages/{stageId}", projectId, stage.id())
                        .header("Authorization", "Bearer " + member.token()))
                .andExpect(status().isNotFound());
    }

    @Test
    void otraEmpresaNoPuedeGestionarEtapas() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        Session other = registerCompanyAndGetSession(uniqueEmail("otro-admin"));
        UUID projectId = createProject(demo.token(), "Proyecto etapas ajeno " + UUID.randomUUID());
        StageRef stage = firstStage(demo.token(), projectId);

        mockMvc.perform(get("/api/projects/{projectId}/stages", projectId)
                        .header("Authorization", "Bearer " + other.token()))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/projects/{projectId}/stages", projectId)
                        .header("Authorization", "Bearer " + other.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"No permitido\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/projects/{projectId}/stages/{stageId}", projectId, stage.id())
                        .header("Authorization", "Bearer " + other.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"No permitido\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/projects/{projectId}/stages/{stageId}", projectId, stage.id())
                        .header("Authorization", "Bearer " + other.token()))
                .andExpect(status().isNotFound());
    }

    @Test
    void nombreVacioDevuelve400() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        UUID projectId = createProject(demo.token(), "Proyecto nombre vacío " + UUID.randomUUID());

        mockMvc.perform(post("/api/projects/{projectId}/stages", projectId)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void eliminarElProyectoBorraSusEtapas() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        UUID projectId = createProject(demo.token(), "Proyecto etapas cascada " + UUID.randomUUID());

        mockMvc.perform(delete("/api/projects/{id}", projectId)
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isNoContent());

        Integer remaining = jdbc.queryForObject(
                "SELECT COUNT(*) FROM project_stages WHERE project_id = ?",
                Integer.class, projectId);
        assertEquals(0, remaining);
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}/stages", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/projects/{projectId}/stages", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Etapa\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCambiaElColorYSeConservaEnStagesYTablero() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        UUID projectId = createProject(demo.token(), "Proyecto color etapa " + UUID.randomUUID());
        StageRef stage = firstStage(demo.token(), projectId);

        mockMvc.perform(put("/api/projects/{projectId}/stages/{stageId}", projectId, stage.id())
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + INITIAL_STAGES.get(0)
                                + "\",\"color\":\"TOSCA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.color").value("TOSCA"));

        mockMvc.perform(get("/api/projects/{projectId}/stages", projectId)
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].color").value("TOSCA"));

        mockMvc.perform(get("/api/tasks/board")
                        .param("projectId", projectId.toString())
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stages[0].color").value("TOSCA"));
    }

    @Test
    void bulkCambiaElColorDeEtapasDeVariosProyectos() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        UUID firstProject = createProject(demo.token(), "Proyecto color A " + UUID.randomUUID());
        UUID secondProject = createProject(demo.token(), "Proyecto color B " + UUID.randomUUID());
        StageRef first = firstStage(demo.token(), firstProject);
        StageRef second = firstStage(demo.token(), secondProject);

        String body = "{\"stages\":["
                + "{\"projectId\":\"" + firstProject + "\",\"stageId\":\"" + first.id()
                + "\",\"color\":\"LAVENDER\"},"
                + "{\"projectId\":\"" + secondProject + "\",\"stageId\":\"" + second.id()
                + "\",\"color\":\"LAVENDER\"}]}";

        mockMvc.perform(put("/api/projects/stages/color")
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects/{projectId}/stages", firstProject)
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].color").value("LAVENDER"));
        mockMvc.perform(get("/api/projects/{projectId}/stages", secondProject)
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].color").value("LAVENDER"));
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

    private StageRef firstStage(String token, UUID projectId) throws Exception {
        String body = mockMvc.perform(get("/api/projects/{projectId}/stages", projectId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return new StageRef(
                UUID.fromString(JsonPath.read(body, "$[0].id")),
                JsonPath.read(body, "$[0].order"));
    }

    private static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@prueba.com";
    }

    private record StageRef(UUID id, int order) { }
    private record Session(String token, UUID userId, String email, UUID tenantId) { }
}
