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

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración de tareas: creación, vinculación al proyecto, permiso
 * granular de creación, acceso por asignación y aislamiento entre empresas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TaskIntegrationTest {

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
    void adminCreaUnaTareaVinculadaAlProyecto() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        UUID projectId = createProject(demo.token(), "Proyecto tareas " + UUID.randomUUID());

        String body = mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Revisar planos\",\"description\":\"Revisar estructura\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").value(projectId.toString()))
                .andExpect(jsonPath("$.title").value("Revisar planos"))
                .andExpect(jsonPath("$.description").value("Revisar estructura"))
                .andExpect(jsonPath("$.status.name").value("Pendiente"))
                .andExpect(jsonPath("$.status.color").value("#EF4444"))
                .andExpect(jsonPath("$.status.isDefault").value(true))
                .andExpect(jsonPath("$.createdBy").value(demo.userId().toString()))
                .andReturn().getResponse().getContentAsString();
        UUID taskId = UUID.fromString(JsonPath.read(body, "$.id"));

        mockMvc.perform(get("/api/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(taskId.toString()));
    }

    @Test
    void workerConPermisoYAsignadoPuedeCrearTarea() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        Session worker = createWorkerSession(DEMO_TENANT, uniqueEmail("worker"));
        UUID projectId = createProject(demo.token(), "Proyecto worker tareas " + UUID.randomUUID());

        mockMvc.perform(post("/api/users/{userId}/task-creation/enable", worker.userId())
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canCreateTasks").value(true));

        assign(demo.token(), projectId, worker.userId());
        Session enabledWorker = login(worker.email(), WORKER_PASSWORD);

        mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + enabledWorker.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Colocar encofrado\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").value(projectId.toString()))
                .andExpect(jsonPath("$.title").value("Colocar encofrado"))
                .andExpect(jsonPath("$.status.name").value("Pendiente"))
                .andExpect(jsonPath("$.createdBy").value(worker.userId().toString()));
    }

    @Test
    void workerSinPermisoNoPuedeCrearTarea() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        Session worker = createWorkerSession(DEMO_TENANT, uniqueEmail("worker"));
        UUID projectId = createProject(demo.token(), "Proyecto sin permiso " + UUID.randomUUID());
        assign(demo.token(), projectId, worker.userId());

        mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + worker.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"No permitido\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void workerConPermisoPeroNoAsignadoNoPuedeCrearTarea() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        Session worker = createWorkerSession(DEMO_TENANT, uniqueEmail("worker"));
        UUID projectId = createProject(demo.token(), "Proyecto ajeno worker " + UUID.randomUUID());

        mockMvc.perform(post("/api/users/{userId}/task-creation/enable", worker.userId())
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + worker.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"No asignado\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void otraEmpresaNoPuedeCrearTarea() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        Session other = registerCompanyAndGetSession(uniqueEmail("otro-admin"));
        UUID projectId = createProject(demo.token(), "Proyecto tareas ajeno " + UUID.randomUUID());

        mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + other.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"No permitido\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + other.token()))
                .andExpect(status().isNotFound());
    }

    @Test
    void tituloVacioDevuelve400() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        UUID projectId = createProject(demo.token(), "Proyecto tareas inválidas " + UUID.randomUUID());

        mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void eliminarElProyectoBorraSusTareas() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        UUID projectId = createProject(demo.token(), "Proyecto borrar tareas " + UUID.randomUUID());

        mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Tarea en cascada\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/projects/{id}", projectId)
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isNoContent());

        Integer remaining = jdbc.queryForObject(
                "SELECT COUNT(*) FROM tasks WHERE project_id = ?",
                Integer.class, projectId);
        assertEquals(0, remaining);
        Integer statuses = jdbc.queryForObject(
                "SELECT COUNT(*) FROM task_statuses WHERE project_id = ?",
                Integer.class, projectId);
        assertEquals(0, statuses);
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/projects/{projectId}/tasks", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/projects/{projectId}/tasks", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Tarea\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/tasks/board"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminGestionaEstadosConColorYNoBorraUnoEnUso() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        UUID projectId = createProject(demo.token(), "Proyecto estados " + UUID.randomUUID());

        String initial = mockMvc.perform(get("/api/projects/{projectId}/task-statuses", projectId)
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].name").value("Pendiente"))
                .andExpect(jsonPath("$[0].color").value("#EF4444"))
                .andExpect(jsonPath("$[0].isDefault").value(true))
                .andExpect(jsonPath("$[2].name").value("Terminado"))
                .andExpect(jsonPath("$[2].isSystem").value(true))
                .andReturn().getResponse().getContentAsString();
        UUID defaultStatusId = UUID.fromString(JsonPath.read(initial, "$[0].id"));
        UUID terminadoStatusId = UUID.fromString(JsonPath.read(initial, "$[2].id"));

        mockMvc.perform(delete("/api/projects/{projectId}/task-statuses/{statusId}",
                        projectId, terminadoStatusId)
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/projects/{projectId}/task-statuses/{statusId}",
                        projectId, defaultStatusId)
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isConflict());

        String created = mockMvc.perform(post("/api/projects/{projectId}/task-statuses", projectId)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"En revisión\",\"color\":\"#8B5CF6\",\"isDefault\":false}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.color").value("#8B5CF6"))
                .andReturn().getResponse().getContentAsString();
        UUID statusId = UUID.fromString(JsonPath.read(created, "$.id"));

        mockMvc.perform(put("/api/projects/{projectId}/task-statuses/{statusId}", projectId, statusId)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Revisión final\",\"color\":\"#A855F7\",\"isDefault\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Revisión final"))
                .andExpect(jsonPath("$.color").value("#A855F7"));

        mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Tarea en revisión\",\"statusId\":\"" + statusId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status.name").value("Revisión final"));

        mockMvc.perform(delete("/api/projects/{projectId}/task-statuses/{statusId}",
                        projectId, statusId)
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/projects/{projectId}/task-statuses", projectId)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Por defecto nuevo\",\"color\":\"#10B981\",\"isDefault\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isDefault").value(true));

        mockMvc.perform(delete("/api/projects/{projectId}/task-statuses/{statusId}",
                        projectId, defaultStatusId)
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminGestionaAreasYAlEliminarlaLaTareaConsultaSinArea() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        UUID projectId = createProject(demo.token(), "Proyecto áreas " + UUID.randomUUID());

        String created = mockMvc.perform(post("/api/projects/{projectId}/areas", projectId)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cimentación\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID areaId = UUID.fromString(JsonPath.read(created, "$.id"));

        mockMvc.perform(put("/api/projects/{projectId}/areas/{areaId}", projectId, areaId)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Estructura\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Estructura"));

        mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Tarea por área\",\"areaId\":\"" + areaId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.area.name").value("Estructura"));

        mockMvc.perform(delete("/api/projects/{projectId}/areas/{areaId}", projectId, areaId)
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].area").doesNotExist());
    }

    @Test
    void asignadoYValidadorDebenEstarVinculadosAlProyecto() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        Session worker = createWorkerSession(DEMO_TENANT, uniqueEmail("worker"));
        Session validator = createWorkerSession(DEMO_TENANT, uniqueEmail("validator"));
        UUID projectId = createProject(demo.token(), "Proyecto usuarios tarea " + UUID.randomUUID());

        mockMvc.perform(post("/api/users/{userId}/task-creation/enable", worker.userId())
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isOk());
        assign(demo.token(), projectId, worker.userId());
        Session enabledWorker = login(worker.email(), WORKER_PASSWORD);

        String task = "{\"title\":\"Tarea con responsables\",\"assigneeUserId\":\""
                + worker.userId() + "\",\"validatorUserId\":\"" + validator.userId() + "\"}";

        mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + enabledWorker.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(task))
                .andExpect(status().isNotFound());

        assign(demo.token(), projectId, validator.userId());

        mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + enabledWorker.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(task))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignee.userId").value(worker.userId().toString()))
                .andExpect(jsonPath("$.validator.userId").value(validator.userId().toString()));
    }

    @Test
    void workerSinPermisoNoPuedeGestionarConfiguracion() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        Session worker = createWorkerSession(DEMO_TENANT, uniqueEmail("worker"));
        UUID projectId = createProject(demo.token(), "Proyecto config " + UUID.randomUUID());
        assign(demo.token(), projectId, worker.userId());

        mockMvc.perform(post("/api/projects/{projectId}/task-statuses", projectId)
                        .header("Authorization", "Bearer " + worker.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bloqueado\",\"color\":\"#000000\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/projects/{projectId}/areas", projectId)
                        .header("Authorization", "Bearer " + worker.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bloqueado\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void areasPredeterminadasDefinenLosCamposDeLaTarea() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        UUID projectId = createProject(demo.token(), "Proyecto áreas tareas " + UUID.randomUUID());

        String areas = mockMvc.perform(get("/api/projects/{projectId}/areas", projectId)
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andReturn().getResponse().getContentAsString();
        List<String> obraIds = JsonPath.read(areas, "$[?(@.name == 'Obra / Ingeniería')].id");
        UUID areaId = UUID.fromString(obraIds.getFirst());

        String tarea = "{\"title\":\"Encofrado\",\"areaId\":\"" + areaId + "\","
                + "\"fechaFinPrevista\":\"2026-12-31\",\"areaDetails\":{"
                + "\"frenteObra\":\"Torre 1 > Piso 3 > Apto 301\","
                + "\"capituloPresupuestal\":\"CIMENTACION\",\"unidadMedida\":\"M2\","
                + "\"cantidadProgramada\":120.5,\"requierePermisoSst\":true,"
                + "\"permisosSst\":[\"TRABAJO_EN_ALTURAS\"]}}";

        String creada = mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tarea))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.area.tipo").value("OBRA_INGENIERIA"))
                .andExpect(jsonPath("$.fechaFinPrevista").value("2026-12-31"))
                .andExpect(jsonPath("$.areaDetails.frenteObra")
                        .value("Torre 1 > Piso 3 > Apto 301"))
                .andExpect(jsonPath("$.areaDetails.permisosSst[0]")
                        .value("TRABAJO_EN_ALTURAS"))
                .andReturn().getResponse().getContentAsString();
        String taskId = JsonPath.read(creada, "$.id");

        mockMvc.perform(get("/api/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(taskId))
                .andExpect(jsonPath("$[0].areaDetails.capituloPresupuestal").value("CIMENTACION"));

        mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Campo ajeno\",\"areaId\":\"" + areaId + "\","
                                + "\"areaDetails\":{\"entidad\":\"Notaría\"}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tareaSeCreaVinculadaAUnaEtapa() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        UUID projectId = createProject(demo.token(), "Proyecto tareas etapa " + UUID.randomUUID());
        UUID stageId = firstStageId(demo.token(), projectId);

        String body = mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Tarea en etapa\",\"stageId\":\"" + stageId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stageId").value(stageId.toString()))
                .andReturn().getResponse().getContentAsString();
        UUID taskId = UUID.fromString(JsonPath.read(body, "$.id"));

        mockMvc.perform(get("/api/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(taskId.toString()))
                .andExpect(jsonPath("$[0].stageId").value(stageId.toString()));
    }

    @Test
    void noCreaTareaConEtapaDeOtroProyecto() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        UUID projectId = createProject(demo.token(), "Proyecto etapa ajena " + UUID.randomUUID());
        UUID otherProjectId = createProject(demo.token(), "Otro proyecto etapa " + UUID.randomUUID());
        UUID foreignStageId = firstStageId(demo.token(), otherProjectId);

        mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Etapa ajena\",\"stageId\":\"" + foreignStageId + "\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void noCreaTareaEnEtapaCerrada() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        UUID projectId = createProject(demo.token(), "Proyecto etapa cerrada " + UUID.randomUUID());
        UUID stageId = firstStageId(demo.token(), projectId);

        mockMvc.perform(post("/api/projects/{projectId}/stages/{stageId}/close", projectId, stageId)
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Etapa cerrada\",\"stageId\":\"" + stageId + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void mueveUnaTareaEntreEtapasYAquiereSinEtapa() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        UUID projectId = createProject(demo.token(), "Proyecto mover tarea " + UUID.randomUUID());
        UUID firstStage = firstStageId(demo.token(), projectId);
        UUID secondStage = stageIdAt(demo.token(), projectId, 1);

        String body = mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Tarea a mover\",\"stageId\":\"" + firstStage + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID taskId = UUID.fromString(JsonPath.read(body, "$.id"));

        mockMvc.perform(patch("/api/tasks/{taskId}/stage", taskId)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stageId\":\"" + secondStage + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId.toString()))
                .andExpect(jsonPath("$.projectId").value(projectId.toString()))
                .andExpect(jsonPath("$.stageId").value(secondStage.toString()));

        mockMvc.perform(patch("/api/tasks/{taskId}/stage", taskId)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stageId\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stageId").doesNotExist());

        mockMvc.perform(get("/api/tasks/board")
                        .param("projectId", projectId.toString())
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks[0].id").value(taskId.toString()))
                .andExpect(jsonPath("$.tasks[0].stageId").doesNotExist());
    }

    @Test
    void noMueveLaTareaAUnaEtapaDeOtroProyecto() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        UUID projectId = createProject(demo.token(), "Proyecto mover ajeno " + UUID.randomUUID());
        UUID otherProjectId = createProject(demo.token(), "Otro proyecto mover " + UUID.randomUUID());
        UUID foreignStageId = firstStageId(demo.token(), otherProjectId);

        String body = mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Tarea con etapa propia\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID taskId = UUID.fromString(JsonPath.read(body, "$.id"));

        mockMvc.perform(patch("/api/tasks/{taskId}/stage", taskId)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stageId\":\"" + foreignStageId + "\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void noMueveLaTareaAUnaEtapaCerrada() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        UUID projectId = createProject(demo.token(), "Proyecto mover cerrada " + UUID.randomUUID());
        UUID stageId = stageIdAt(demo.token(), projectId, 1);
        mockMvc.perform(post("/api/projects/{projectId}/stages/{stageId}/close", projectId, stageId)
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isOk());

        String body = mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Tarea sin mover\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID taskId = UUID.fromString(JsonPath.read(body, "$.id"));

        mockMvc.perform(patch("/api/tasks/{taskId}/stage", taskId)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stageId\":\"" + stageId + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void eliminarEtapaDejaLaTareaSinEtapa() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        UUID projectId = createProject(demo.token(), "Proyecto borrar etapa tarea " + UUID.randomUUID());
        UUID stageId = firstStageId(demo.token(), projectId);

        String body = mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Tarea con etapa\",\"stageId\":\"" + stageId + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID taskId = UUID.fromString(JsonPath.read(body, "$.id"));

        mockMvc.perform(delete("/api/projects/{projectId}/stages/{stageId}", projectId, stageId)
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(taskId.toString()))
                .andExpect(jsonPath("$[0].stageId").doesNotExist());
    }

    @Test
    void tableroDevuelveEtapasYTareasDelProyecto() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        UUID projectId = createProject(demo.token(), "Proyecto tablero " + UUID.randomUUID());
        UUID stageId = firstStageId(demo.token(), projectId);
        mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Tarea del tablero\",\"stageId\":\"" + stageId + "\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tasks/board")
                        .param("projectId", projectId.toString())
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stages", hasSize(6)))
                .andExpect(jsonPath("$.stages[0].projectId").value(projectId.toString()))
                .andExpect(jsonPath("$.tasks[0].projectId").value(projectId.toString()))
                .andExpect(jsonPath("$.tasks[0].stageId").value(stageId.toString()))
                .andExpect(jsonPath("$.tasks[0].status.name").value("Pendiente"))
                .andExpect(jsonPath("$.truncated").value(false));
    }

    @Test
    void tableroRespetaLaVisibilidadDelProyecto() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        Session worker = createWorkerSession(DEMO_TENANT, uniqueEmail("tablero"));
        UUID visibleId = createProject(demo.token(), "Proyecto visible " + UUID.randomUUID());
        UUID hiddenId = createProject(demo.token(), "Proyecto oculto " + UUID.randomUUID());
        mockMvc.perform(post("/api/projects/{projectId}/tasks", visibleId)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Tarea visible\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/projects/{projectId}/tasks", hiddenId)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Tarea oculta\"}"))
                .andExpect(status().isCreated());
        assign(demo.token(), visibleId, worker.userId());

        mockMvc.perform(get("/api/tasks/board")
                        .header("Authorization", "Bearer " + worker.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks", hasSize(1)))
                .andExpect(jsonPath("$.tasks[0].projectId").value(visibleId.toString()));

        mockMvc.perform(get("/api/tasks/board")
                        .param("projectId", hiddenId.toString())
                        .header("Authorization", "Bearer " + worker.token()))
                .andExpect(status().isNotFound());
    }

    @Test
    void tableroAvisaCuandoRecortaPorElLimite() throws Exception {
        Session demo = login("admin@demo.com", "Admin123!");
        UUID projectId = createProject(demo.token(), "Proyecto límite tablero " + UUID.randomUUID());
        mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Primera tarea\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/projects/{projectId}/tasks", projectId)
                        .header("Authorization", "Bearer " + demo.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Segunda tarea\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tasks/board")
                        .param("projectId", projectId.toString())
                        .param("limit", "1")
                        .header("Authorization", "Bearer " + demo.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks", hasSize(1)))
                .andExpect(jsonPath("$.truncated").value(true));
    }

    private UUID firstStageId(String token, UUID projectId) throws Exception {
        return stageIdAt(token, projectId, 0);
    }

    private UUID stageIdAt(String token, UUID projectId, int index) throws Exception {
        String body = mockMvc.perform(get("/api/projects/{projectId}/stages", projectId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$[" + index + "].id"));
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

    private Session createWorkerSession(UUID tenantId, String email) throws Exception {
        UUID userId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO users (id, email, full_name, password_hash, active)
                VALUES (?, ?, ?, ?, TRUE)
                """,
                userId, email, "Trabajador " + email,
                passwordEncoder.encode(WORKER_PASSWORD));
        jdbc.update("""
                INSERT INTO memberships (id, user_id, tenant_id, role, active)
                VALUES (?, ?, ?, 'WORKER', TRUE)
                """,
                UUID.randomUUID(), userId, tenantId);
        return login(email, WORKER_PASSWORD);
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
