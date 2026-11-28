package com.SaaS.Constructora.projects.interfaces;

import com.SaaS.Constructora.projects.application.TaskService;
import com.SaaS.Constructora.shared.security.AuthenticatedUser;
import com.SaaS.Constructora.shared.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Tablero global de tareas y cambio de etapa de una tarjeta. La visibilidad se
 * resuelve en el servicio con el tenant y el rol del usuario autenticado.
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskBoardController {

    private final TaskService service;

    TaskBoardController(TaskService service) { this.service = service; }

    @GetMapping("/board")
    TaskBoardResponse board(
            @CurrentUser AuthenticatedUser user,
            @RequestParam(required = false) UUID projectId,
            @RequestParam(defaultValue = "500") int limit) {
        return service.getBoard(user.tenantId(), user.userId(), user.role(), projectId, limit);
    }

    /** Edita los campos de una tarea existente; el proyecto de la tarea no cambia. */
    @PutMapping("/{taskId}")
    TaskResponse update(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID taskId,
            @Valid @RequestBody CreateTaskRequest request) {
        return service.update(user.tenantId(), taskId, user.userId(), user.role(), request);
    }

    /** Mueve una tarea a otra etapa de su proyecto; `stageId` nulo la deja sin etapa. */
    @PatchMapping("/{taskId}/stage")
    TaskResponse moveToStage(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID taskId,
            @Valid @RequestBody MoveTaskRequest request) {
        return service.moveToStage(
                user.tenantId(), taskId, user.userId(), user.role(), request.stageId());
    }
}
