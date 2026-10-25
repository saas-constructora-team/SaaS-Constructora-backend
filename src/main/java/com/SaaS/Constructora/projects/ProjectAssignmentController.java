package com.SaaS.Constructora.projects;

import com.SaaS.Constructora.shared.security.AuthenticatedUser;
import com.SaaS.Constructora.shared.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Asignaciones de personal a un proyecto. Solo ADMIN puede consultarlas,
 * crearlas o eliminarlas; el tenant siempre se toma del JWT.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/assignments")
class ProjectAssignmentController {

    private final ProjectAssignmentService service;

    ProjectAssignmentController(ProjectAssignmentService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<ProjectAssignmentResponse> assign(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID projectId,
            @Valid @RequestBody AssignProjectRequest request) {
        user.requireAdmin();
        ProjectAssignmentResponse response = service.assign(
                user.tenantId(), projectId, request.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    List<ProjectAssignmentResponse> list(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID projectId) {
        user.requireAdmin();
        return service.list(user.tenantId(), projectId);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unassign(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID projectId,
            @PathVariable UUID userId) {
        user.requireAdmin();
        service.unassign(user.tenantId(), projectId, userId);
    }
}
