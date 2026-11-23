package com.SaaS.Constructora.audit;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Extremo a extremo: eventos de projects -> consumidor audit -> búsqueda. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuditIntegrationTest {

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
    void crearProyectoGeneraUnaEntradaDeAuditoriaDelTenant() throws Exception {
        Session admin = registerCompanyAndGetSession(uniqueEmail("admin-audit"));
        UUID projectId = createProject(admin.token(), "Proyecto auditado " + UUID.randomUUID());

        String body = awaitAuditEvent(admin.token(), "ProjectCreated");

        assertEquals(admin.tenantId().toString(), JsonPath.read(body, "$.content[0].tenantId"));
        assertEquals(admin.userId().toString(), JsonPath.read(body, "$.content[0].actorUserId"));
        assertEquals(projectId.toString(), JsonPath.read(body, "$.content[0].entityId"));
        assertEquals("PROJECT", JsonPath.read(body, "$.content[0].entityType"));

        Session otroTenant = registerCompanyAndGetSession(uniqueEmail("admin-otro"));
        mockMvc.perform(get("/api/audit/events")
                        .header("Authorization", "Bearer " + otroTenant.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void cerrarEtapaPublicaElEventoYBloqueaUnSegundoCierre() throws Exception {
        Session admin = registerCompanyAndGetSession(uniqueEmail("admin-close"));
        UUID projectId = createProject(admin.token(), "Proyecto cierre " + UUID.randomUUID());
        String stages = mockMvc.perform(get("/api/projects/{projectId}/stages", projectId)
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        UUID stageId = UUID.fromString(JsonPath.read(stages, "$[0].id"));

        mockMvc.perform(post("/api/projects/{projectId}/stages/{stageId}/close", projectId, stageId)
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(stageId.toString()))
                .andExpect(jsonPath("$.closedAt").isNotEmpty());

        String body = awaitAuditEvent(admin.token(), "ProjectStageClosed");
        assertEquals(stageId.toString(), JsonPath.read(body, "$.content[0].entityId"));

        mockMvc.perform(post("/api/projects/{projectId}/stages/{stageId}/close", projectId, stageId)
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/projects/{projectId}/stages/{stageId}", projectId, stageId)
                        .header("Authorization", "Bearer " + admin.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"No deberia cambiar\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void busquedaDeProyectosFiltraYPagina() throws Exception {
        Session admin = registerCompanyAndGetSession(uniqueEmail("admin-search"));
        String unique = UUID.randomUUID().toString().substring(0, 8);
        createProject(admin.token(), "Edificio Aurora " + unique);
        createProject(admin.token(), "Casa Becerra " + unique);

        mockMvc.perform(get("/api/projects/search")
                        .param("query", "aurora")
                        .param("page", "0")
                        .param("size", "1")
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Edificio Aurora " + unique));
    }

    @Test
    void memberNoPuedeConsultarLaAuditoria() throws Exception {
        Session admin = registerCompanyAndGetSession(uniqueEmail("admin-permisos"));
        Session member = createMemberSession(admin.tenantId(), uniqueEmail("member-audit"));

        mockMvc.perform(get("/api/audit/events")
                        .header("Authorization", "Bearer " + member.token()))
                .andExpect(status().isForbidden());
    }

    /** El listener de Modulith es asíncrono: se espera a que la entrada aparezca. */
    private String awaitAuditEvent(String token, String eventType) throws Exception {
        for (int attempt = 0; attempt < 60; attempt++) {
            String body = mockMvc.perform(get("/api/audit/events")
                            .param("eventType", eventType)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            Integer total = JsonPath.read(body, "$.totalElements");
            if (total != null && total > 0) {
                return body;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("No apareció la entrada de auditoría para " + eventType);
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
        return new Session(
                JsonPath.read(body, "$.accessToken"),
                UUID.fromString(JsonPath.read(body, "$.user.id")),
                UUID.fromString(JsonPath.read(body, "$.tenant.id")));
    }

    private Session createMemberSession(UUID tenantId, String email) throws Exception {
        UUID userId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO users (id, email, full_name, password_hash, active)
                VALUES (?, ?, ?, ?, TRUE)
                """,
                userId, email, "Miembro " + email, passwordEncoder.encode("Secreta123!"));
        jdbc.update("""
                INSERT INTO memberships (id, user_id, tenant_id, role, active)
                VALUES (?, ?, ?, 'MEMBER', TRUE)
                """,
                UUID.randomUUID(), userId, tenantId);
        return login(email, "Secreta123!");
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

    private static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@prueba.com";
    }

    private record Session(String token, UUID userId, UUID tenantId) { }
}
