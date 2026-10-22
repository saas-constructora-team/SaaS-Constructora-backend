package com.SaaS.Constructora.projects;

import com.SaaS.Constructora.shared.security.TenantClaims;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID projectId,
            @Valid @RequestBody AssignProjectRequest request) {
        TenantClaims.requireAdmin(jwt);
        UUID tenantId = TenantClaims.requireTenantId(jwt);
        ProjectAssignmentResponse response = service.assign(
                tenantId, projectId, request.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    List<ProjectAssignmentResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID projectId) {
        TenantClaims.requireAdmin(jwt);
        UUID tenantId = TenantClaims.requireTenantId(jwt);
        return service.list(tenantId, projectId);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unassign(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID projectId,
            @PathVariable UUID userId) {
        TenantClaims.requireAdmin(jwt);
        UUID tenantId = TenantClaims.requireTenantId(jwt);
        service.unassign(tenantId, projectId, userId);
    }
}
