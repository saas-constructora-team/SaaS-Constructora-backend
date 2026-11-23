package com.SaaS.Constructora.projects.interfaces;
import com.SaaS.Constructora.projects.application.ProjectAssignmentService;

import com.SaaS.Constructora.shared.security.AuthenticatedUser;
import com.SaaS.Constructora.shared.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Asignaciones de personal a un proyecto y delegación de su gestión.
 * Un ADMIN del tenant puede operar sobre cualquier proyecto; un MEMBER solo
 * si fue nombrado LEAD del proyecto. El tenant siempre se toma del JWT.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/assignments")
public class ProjectAssignmentController {

    private final ProjectAssignmentService service;

    ProjectAssignmentController(ProjectAssignmentService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<ProjectAssignmentResponse> assign(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID projectId,
            @Valid @RequestBody AssignProjectRequest request) {
        ProjectAssignmentResponse response = service.assign(
                user.tenantId(), projectId, request.userId(),
                request.roleOrDefault(), user.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{userId}")
    ProjectAssignmentResponse updateRole(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID projectId,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateProjectAssignmentRequest request) {
        return service.updateRole(
                user.tenantId(), projectId, userId, request.role(), user.userId());
    }

    @GetMapping
    List<ProjectAssignmentResponse> list(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID projectId) {
        return service.list(user.tenantId(), projectId, user.userId());
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unassign(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID projectId,
            @PathVariable UUID userId) {
        service.unassign(user.tenantId(), projectId, userId, user.userId());
    }
}
