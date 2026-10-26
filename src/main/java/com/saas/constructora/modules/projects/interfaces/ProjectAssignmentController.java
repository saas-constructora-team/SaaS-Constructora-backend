package com.saas.constructora.modules.projects.interfaces;

import com.saas.constructora.modules.projects.application.ProjectAssignmentService;
import com.saas.constructora.modules.projects.interfaces.dto.AssignProjectRequest;
import com.saas.constructora.modules.projects.interfaces.dto.ProjectAssignmentResponse;
import com.saas.constructora.shared.security.TenantClaims;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/assignments")
class ProjectAssignmentController {
    private final ProjectAssignmentService service;
    ProjectAssignmentController(ProjectAssignmentService service) { this.service = service; }

    @PostMapping
    ResponseEntity<ProjectAssignmentResponse> assign(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID projectId, @Valid @RequestBody AssignProjectRequest request) {
        TenantClaims.requireAdmin(jwt);
        ProjectAssignmentResponse response = service.assign(
                TenantClaims.requireTenantId(jwt), projectId, request.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    List<ProjectAssignmentResponse> list(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID projectId) {
        TenantClaims.requireAdmin(jwt);
        return service.list(TenantClaims.requireTenantId(jwt), projectId);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void unassign(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID projectId,
            @PathVariable UUID userId) {
        TenantClaims.requireAdmin(jwt);
        service.unassign(TenantClaims.requireTenantId(jwt), projectId, userId);
    }
}
