package com.SaaS.Constructora.projects.interfaces;
import com.SaaS.Constructora.projects.application.TaskStatusService;

import com.SaaS.Constructora.shared.security.AuthenticatedUser;
import com.SaaS.Constructora.shared.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Catálogo de estados de tareas de un proyecto. */
@RestController
@RequestMapping("/api/projects/{projectId}/task-statuses")
public class TaskStatusController {

    private final TaskStatusService service;

    TaskStatusController(TaskStatusService service) { this.service = service; }

    @GetMapping
    List<TaskStatusResponse> list(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID projectId) {
        return service.list(user.tenantId(), projectId, user.userId(), user.role());
    }

    @PostMapping
    ResponseEntity<TaskStatusResponse> create(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateTaskStatusRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                service.create(user.tenantId(), projectId, user.userId(), user.role(), request));
    }

    @PutMapping("/{statusId}")
    TaskStatusResponse update(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID projectId,
            @PathVariable UUID statusId,
            @Valid @RequestBody UpdateTaskStatusRequest request) {
        return service.update(user.tenantId(), projectId, statusId,
                user.userId(), user.role(), request);
    }

    @DeleteMapping("/{statusId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID projectId,
            @PathVariable UUID statusId) {
        service.delete(user.tenantId(), projectId, statusId, user.userId(), user.role());
    }
}
