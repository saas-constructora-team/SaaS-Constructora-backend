package com.SaaS.Constructora.documents;

import com.SaaS.Constructora.documents.application.ports.ObjectStoragePort;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Recorrido completo de HU-04 sobre PostgreSQL real. El almacenamiento se
 * sustituye por un puerto en memoria: la integración con S3 se comprueba en el
 * adaptador y aquí se validan tenant, roles, versiones y contrato HTTP.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Import(DocumentIntegrationTest.FakeStorageConfig.class)
class DocumentIntegrationTest {

    private static final byte[] PNG = new byte[]{
            (byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 1, 2, 3, 4, 5, 6, 7, 8
    };

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecordingObjectStorage storage;

    @Test
    void cargaVersionaYDescargaUnDocumentoDelProyecto() throws Exception {
        String token = loginDemo();
        UUID projectId = createProject(token);

        String body = mockMvc.perform(multipart("/api/projects/{projectId}/documents", projectId)
                        .file(file("planta.png", "image/png", PNG))
                        .file(metadata("""
                                {"title":"Planta arquitectónica","kind":"IMAGE","versionLabel":"v1.0"}
                                """))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.kind").value("IMAGE"))
                .andExpect(jsonPath("$.currentVersionId").exists())
                .andReturn().getResponse().getContentAsString();
        UUID documentId = UUID.fromString(JsonPath.read(body, "$.id"));

        mockMvc.perform(multipart("/api/projects/{projectId}/documents/{documentId}/versions",
                        projectId, documentId)
                        .file(file("planta-v2.png", "image/png", PNG))
                        .file(metadata("{\"versionLabel\":\"v2.0\"}"))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.versionLabel").value("v2.0"));

        mockMvc.perform(get("/api/projects/{projectId}/documents/{documentId}/versions",
                        projectId, documentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/api/projects/{projectId}/documents/{documentId}/versions/{versionId}/download",
                        projectId, documentId, JsonPath.read(body, "$.currentVersionId"))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isFound())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.startsWith("https://firma.local/tenants/")));
    }

    @Test
    void sobrescribeLaVersionVigenteYPodaLosBinariosAntiguos() throws Exception {
        storage.clear();
        String token = loginDemo();
        UUID projectId = createProject(token);

        String created = mockMvc.perform(multipart("/api/projects/{projectId}/documents", projectId)
                        .file(file("planta.png", "image/png", PNG))
                        .file(metadata("""
                                {"title":"Planta arquitectónica","kind":"IMAGE","versionLabel":"v1.0"}
                                """))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID documentId = UUID.fromString(JsonPath.read(created, "$.id"));
        UUID firstVersionId = UUID.fromString(JsonPath.read(created, "$.currentVersionId"));

        addVersion(token, projectId, documentId, "v2.0");
        overwrite(token, projectId, documentId);
        addVersion(token, projectId, documentId, "v3.0");
        overwrite(token, projectId, documentId);
        addVersion(token, projectId, documentId, "v4.0");

        assertEquals(3, storage.keys().size());

        String history = mockMvc.perform(get("/api/projects/{projectId}/documents/{documentId}/versions",
                        projectId, documentId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> versions = JsonPath.read(history, "$");
        assertEquals(4, versions.size());
        Map<String, Object> first = versions.stream()
                .filter(version -> "v1.0".equals(version.get("versionLabel")))
                .findFirst().orElseThrow();
        assertEquals(false, first.get("binaryAvailable"));

        mockMvc.perform(get("/api/projects/{projectId}/documents/{documentId}/versions/{versionId}/download",
                        projectId, documentId, firstVersionId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isGone());
    }

    @Test
    void otroTenantNoPuedeCargarDocumentosEnElProyecto() throws Exception {
        String demoToken = loginDemo();
        UUID projectId = createProject(demoToken);
        String otherToken = registerCompanyAndGetToken();

        mockMvc.perform(multipart("/api/projects/{projectId}/documents", projectId)
                        .file(file("planta.png", "image/png", PNG))
                        .file(metadata("""
                                {"title":"Planta","kind":"IMAGE","versionLabel":"v1.0"}
                                """))
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void rechazaUnArchivoCuyaFirmaNoCorresponde() throws Exception {
        String token = loginDemo();
        UUID projectId = createProject(token);
        byte[] ejecutable = "MZejecutable".getBytes(StandardCharsets.US_ASCII);

        mockMvc.perform(multipart("/api/projects/{projectId}/documents", projectId)
                        .file(file("planta.png", "image/png", ejecutable))
                        .file(metadata("""
                                {"title":"Planta","kind":"IMAGE","versionLabel":"v1.0"}
                                """))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnsupportedMediaType());
    }

    private void addVersion(String token, UUID projectId, UUID documentId, String label) throws Exception {
        mockMvc.perform(multipart("/api/projects/{projectId}/documents/{documentId}/versions",
                        projectId, documentId)
                        .file(file(label + ".png", "image/png", PNG))
                        .file(metadata("{\"versionLabel\":\"" + label + "\"}"))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());
    }

    private void overwrite(String token, UUID projectId, UUID documentId) throws Exception {
        mockMvc.perform(multipart(HttpMethod.PUT,
                        "/api/projects/{projectId}/documents/{documentId}/current", projectId, documentId)
                        .file(file("sobrescrita.png", "image/png", PNG))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.binaryAvailable").value(true));
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

    private MockMultipartFile file(String name, String contentType, byte[] content) {
        return new MockMultipartFile("file", name, contentType, content);
    }

    private MockMultipartFile metadata(String json) {
        return new MockMultipartFile("metadata", "", MediaType.APPLICATION_JSON_VALUE,
                json.getBytes(StandardCharsets.UTF_8));
    }

    @TestConfiguration
    static class FakeStorageConfig {
        @Bean
        RecordingObjectStorage recordingObjectStorage() {
            return new RecordingObjectStorage();
        }

        @Bean
        @Primary
        ObjectStoragePort fakeObjectStorage(RecordingObjectStorage storage) {
            return storage;
        }
    }

    /** Almacenamiento en memoria que permite comprobar la retención de objetos. */
    static class RecordingObjectStorage implements ObjectStoragePort {
        private final Set<String> keys = ConcurrentHashMap.newKeySet();

        @Override
        public void store(String key, InputStream content, long size, String contentType) {
            keys.add(key);
        }

        @Override
        public String presignDownload(String key, String originalName) {
            return "https://firma.local/" + key;
        }

        @Override
        public void delete(String key) {
            keys.remove(key);
        }

        Set<String> keys() {
            return Set.copyOf(keys);
        }

        void clear() {
            keys.clear();
        }
    }
}
