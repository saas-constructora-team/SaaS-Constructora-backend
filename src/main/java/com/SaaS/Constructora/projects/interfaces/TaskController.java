package com.SaaS.Constructora.projects.interfaces;
import com.SaaS.Constructora.projects.application.TaskService;

import com.SaaS.Constructora.shared.security.AuthenticatedUser;
import com.SaaS.Constructora.shared.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Tareas de un proyecto. Requieren permiso de creación en la membresía y acceso
 * al proyecto: ADMIN puede gestionar cualquier proyecto de su empresa; un
 * WORKER o MEMBER solo los proyectos que tiene asignados.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
public class TaskController {

    private final TaskService service;

    TaskController(TaskService service) { this.service = service; }

    @GetMapping
    List<TaskResponse> list(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID projectId) {
        return service.list(
                user.tenantId(),
                projectId,
                user.userId(),
                user.role());
    }

    @PostMapping
    ResponseEntity<TaskResponse> create(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateTaskRequest request) {
        TaskResponse response = service.create(
                user.tenantId(),
                projectId,
                user.userId(),
                user.role(),
                request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
