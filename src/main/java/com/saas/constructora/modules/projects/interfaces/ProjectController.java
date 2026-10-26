package com.saas.constructora.modules.projects.interfaces;

import com.saas.constructora.modules.projects.application.ProjectService;
import com.saas.constructora.modules.projects.interfaces.dto.CreateProjectRequest;
import com.saas.constructora.modules.projects.interfaces.dto.ProjectResponse;
import com.saas.constructora.modules.projects.interfaces.dto.UpdateProjectRequest;
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
@RequestMapping("/api/projects")
class ProjectController {
    private final ProjectService service;
    ProjectController(ProjectService service) { this.service = service; }

    @PostMapping
    ResponseEntity<ProjectResponse> create(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(
                TenantClaims.requireTenantId(jwt), TenantClaims.requireRole(jwt), request));
    }

    @GetMapping
    List<ProjectResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return service.list(TenantClaims.requireTenantId(jwt), TenantClaims.requireUserId(jwt),
                TenantClaims.requireRole(jwt));
    }

    @PutMapping("/{id}")
    ProjectResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @Valid @RequestBody UpdateProjectRequest request) {
        return service.update(TenantClaims.requireTenantId(jwt), id,
                TenantClaims.requireUserId(jwt), TenantClaims.requireRole(jwt), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        service.delete(TenantClaims.requireTenantId(jwt), id,
                TenantClaims.requireUserId(jwt), TenantClaims.requireRole(jwt));
    }
}
