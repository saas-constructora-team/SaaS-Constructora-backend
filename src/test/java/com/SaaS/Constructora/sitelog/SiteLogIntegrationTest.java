package com.SaaS.Constructora.sitelog;

import com.SaaS.Constructora.documents.application.ports.ObjectStoragePort;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Recorrido real sobre PostgreSQL: pertenencia, etapa abierta, unicidad y listado. */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(SiteLogIntegrationTest.FakeStorageConfig.class)
class SiteLogIntegrationTest {

    private static final byte[] PNG = new byte[]{
            (byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 1, 2, 3, 4, 5, 6, 7, 8
    };

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Test
    void asociaUnDocumentoALaEtapaYLoLista() throws Exception {
        String token = loginDemo();
        UUID projectId = createProject(token);
        UUID stageId = firstStageId(token, projectId);
        UUID documentId = uploadDocument(token, projectId);

        mockMvc.perform(post("/api/projects/{projectId}/stages/{stageId}/documents",
                        projectId, stageId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentId\":\"" + documentId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentId").value(documentId.toString()))
                .andExpect(jsonPath("$.stageId").value(stageId.toString()));

        mockMvc.perform(get("/api/projects/{projectId}/stages/{stageId}/documents",
                        projectId, stageId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].documentId").value(documentId.toString()));

        mockMvc.perform(post("/api/projects/{projectId}/stages/{stageId}/documents",
                        projectId, stageId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentId\":\"" + documentId + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void otroTenantNoPuedeAsociarDocumentosALaEtapa() throws Exception {
        String demoToken = loginDemo();
        UUID projectId = createProject(demoToken);
        UUID stageId = firstStageId(demoToken, projectId);
        UUID documentId = uploadDocument(demoToken, projectId);
        String otherToken = registerCompanyAndGetToken();

        mockMvc.perform(post("/api/projects/{projectId}/stages/{stageId}/documents",
                        projectId, stageId)
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentId\":\"" + documentId + "\"}"))
                .andExpect(status().isNotFound());
    }

    private String loginDemo() throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@demo.com\",\"password\":\"Admin123!\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }

    private String registerCompanyAndGetToken() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Constructora " + suffix + "\","
                                + "\"taxId\":\"NIT-" + suffix + "\","
                                + "\"address\":\"Calle Prueba 123\","
                                + "\"adminFullName\":\"Admin Prueba\","
                                + "\"adminEmail\":\"admin-" + suffix + "@prueba.com\","
                                + "\"adminPassword\":\"Secreta123!\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }

    private UUID createProject(String token) throws Exception {
        String response = mockMvc.perform(post("/api/projects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Obra " + UUID.randomUUID() + "\","
                                + "\"clientName\":\"Inmobiliaria XYZ\",\"location\":\"Tuluá\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(response, "$.id"));
    }

    private UUID firstStageId(String token, UUID projectId) throws Exception {
        String response = mockMvc.perform(get("/api/projects/{projectId}/stages", projectId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(response, "$[0].id"));
    }

    private UUID uploadDocument(String token, UUID projectId) throws Exception {
        String response = mockMvc.perform(multipart("/api/projects/{projectId}/documents", projectId)
                        .file(new MockMultipartFile("file", "planta.png", "image/png", PNG))
                        .file(new MockMultipartFile("metadata", "", MediaType.APPLICATION_JSON_VALUE,
                                "{\"title\":\"Planta\",\"kind\":\"IMAGE\",\"versionLabel\":\"v1.0\"}"
                                        .getBytes(StandardCharsets.UTF_8)))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(response, "$.id"));
    }

    @TestConfiguration
    static class FakeStorageConfig {
        @Bean
        @Primary
        ObjectStoragePort fakeObjectStorage() {
            return new ObjectStoragePort() {
                @Override
                public void store(String key, InputStream content, long size, String contentType) {
                    // No-op: la integración valida metadatos, no S3 real.
                }

                @Override
                public String presignDownload(String key, String originalName) {
                    return "https://firma.local/" + key;
                }

                @Override
                public void delete(String key) {
                    // No-op.
                }
            };
        }
    }
}
