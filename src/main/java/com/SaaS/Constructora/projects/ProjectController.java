package com.SaaS.Constructora.projects;

import com.SaaS.Constructora.shared.security.AuthenticatedUser;
import com.SaaS.Constructora.shared.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
            @CurrentUser AuthenticatedUser user,
            @Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(user.tenantId(), user.role(), request));
    }

    @GetMapping
    List<ProjectResponse> list(@CurrentUser AuthenticatedUser user) {
        return service.list(user.tenantId(), user.userId(), user.role());
    }

    @PutMapping("/{id}")
    ProjectResponse update(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProjectRequest request) {
        return service.update(
                user.tenantId(), id, user.userId(), user.role(), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@CurrentUser AuthenticatedUser user, @PathVariable UUID id) {
        service.delete(user.tenantId(), id, user.userId(), user.role());
    }
}
