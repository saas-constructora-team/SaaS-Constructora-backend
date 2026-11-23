package com.SaaS.Constructora.projects.interfaces;
import com.SaaS.Constructora.projects.application.ProjectAreaService;

import com.SaaS.Constructora.shared.security.AuthenticatedUser;
import com.SaaS.Constructora.shared.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Áreas de trabajo de un proyecto. */
@RestController
@RequestMapping("/api/projects/{projectId}/areas")
public class ProjectAreaController {

    private final ProjectAreaService service;

    ProjectAreaController(ProjectAreaService service) { this.service = service; }

    @GetMapping
    List<ProjectAreaResponse> list(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID projectId) {
        return service.list(user.tenantId(), projectId, user.userId(), user.role());
    }

    @PostMapping
    ResponseEntity<ProjectAreaResponse> create(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateProjectAreaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                service.create(user.tenantId(), projectId, user.userId(), user.role(), request));
    }

    @PutMapping("/{areaId}")
    ProjectAreaResponse update(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID projectId,
            @PathVariable UUID areaId,
            @Valid @RequestBody UpdateProjectAreaRequest request) {
        return service.update(user.tenantId(), projectId, areaId,
                user.userId(), user.role(), request);
    }

    @DeleteMapping("/{areaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID projectId,
            @PathVariable UUID areaId) {
        service.delete(user.tenantId(), projectId, areaId, user.userId(), user.role());
    }
}
