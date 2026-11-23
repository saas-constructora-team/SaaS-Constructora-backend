package com.SaaS.Constructora.projects.interfaces;
import com.SaaS.Constructora.projects.application.ProjectService;
import com.SaaS.Constructora.projects.domain.ProjectStatus;
import com.SaaS.Constructora.projects.domain.ProjectType;

import com.SaaS.Constructora.shared.security.AuthenticatedUser;
import com.SaaS.Constructora.shared.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService service;

    ProjectController(ProjectService service) { this.service = service; }

    @PostMapping
    ResponseEntity<ProjectResponse> create(
            @CurrentUser AuthenticatedUser user,
            @Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(user.tenantId(), user.userId(), user.role(), request));
    }

    @GetMapping
    List<ProjectResponse> list(@CurrentUser AuthenticatedUser user) {
        return service.list(user.tenantId(), user.userId(), user.role());
    }

    /** Búsqueda paginada: el orden permitido se fija en el servicio. */
    @GetMapping("/search")
    Page<ProjectResponse> search(
            @CurrentUser AuthenticatedUser user,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) ProjectStatus status,
            @RequestParam(required = false) ProjectType projectType,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.search(user.tenantId(), user.userId(), user.role(), query,
                status, projectType, from, to,
                PageRequest.of(Math.max(page, 0), Math.max(size, 1)));
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
