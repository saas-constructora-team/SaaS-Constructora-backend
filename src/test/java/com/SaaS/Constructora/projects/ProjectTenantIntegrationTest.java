package com.SaaS.Constructora.projects;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica que un proyecto quede vinculado a la empresa (tenant) del JWT y
 * que el aislamiento multi-tenant no dependa de headers enviados por el cliente.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ProjectTenantIntegrationTest {

    private static final UUID DEMO_TENANT = UUID.fromString("5f2b9c1d-3e47-4a86-9c21-8d4f0a7b5e13");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void crearProyectoUsaElTenantDelToken() throws Exception {
        String token = loginDemo();
        String name = "Proyecto " + UUID.randomUUID();

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectBody(name)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value(DEMO_TENANT.toString()))
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem(name)));
    }

    @Test
    void otraEmpresaNoVeLosProyectosDeLaPrimera() throws Exception {
        String demoToken = loginDemo();
        String demoProject = "Proyecto demo " + UUID.randomUUID();
        createProject(demoToken, demoProject);

        String otherToken = registerCompanyAndGetToken();

        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", not(hasItem(demoProject))));
    }

    @Test
    void elHeaderXTenantIdNoCambiaLaEmpresaDelProyecto() throws Exception {
        String token = loginDemo();
        String name = "Proyecto sin spoofing " + UUID.randomUUID();

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectBody(name)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value(DEMO_TENANT.toString()));
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectBody("Proyecto " + UUID.randomUUID())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalidoDevuelve401() throws Exception {
        mockMvc.perform(get("/api/projects")
                        .header("Authorization", "Bearer no-es-un-jwt"))
                .andExpect(status().isUnauthorized());
    }

    private String loginDemo() throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@demo.com\",\"password\":\"Admin123!\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private String registerCompanyAndGetToken() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Constructora " + suffix + "\","
                                + "\"adminFullName\":\"Admin Prueba\","
                                + "\"adminEmail\":\"admin-" + suffix + "@prueba.com\","
                                + "\"adminPassword\":\"Secreta123\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private void createProject(String token, String name) throws Exception {
        mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(projectBody(name)))
                .andExpect(status().isCreated());
    }

    private static String projectBody(String name) {
        return "{\"name\":\"" + name + "\",\"clientName\":\"Inmobiliaria XYZ\","
                + "\"location\":\"Tuluá, Valle del Cauca\"}";
    }
}
