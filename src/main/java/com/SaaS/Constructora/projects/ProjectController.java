package com.SaaS.Constructora.projects;

import com.SaaS.Constructora.shared.security.TenantClaims;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
class ProjectController {

    private final ProjectService service;

    ProjectController(ProjectService service) { this.service = service; }

    @PostMapping
    ResponseEntity<ProjectResponse> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateProjectRequest request) {
        UUID tenantId = TenantClaims.requireTenantId(jwt);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(tenantId, request));
    }

    @GetMapping
    List<ProjectResponse> list(@AuthenticationPrincipal Jwt jwt) {
        UUID tenantId = TenantClaims.requireTenantId(jwt);
        return service.list(tenantId);
    }
}
