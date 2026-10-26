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
 * Etapas de un proyecto. ADMIN puede gestionar cualquier proyecto de su
 * empresa; un MEMBER solo los proyectos que tiene asignados.
 */
@RestController
@RequestMapping("/api/projects/{projectId}/stages")
class ProjectStageController {

    private final ProjectStageService service;

    ProjectStageController(ProjectStageService service) {
        this.service = service;
    }

    @GetMapping
    List<ProjectStageResponse> list(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID projectId) {
        return service.list(
                user.tenantId(),
                projectId,
                user.userId(),
                user.role());
    }

    @PostMapping
    ResponseEntity<ProjectStageResponse> create(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID projectId,
            @Valid @RequestBody CreateProjectStageRequest request) {
        ProjectStageResponse response = service.create(
                user.tenantId(),
                projectId,
                user.userId(),
                user.role(),
                request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{stageId}")
    ProjectStageResponse update(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID projectId,
            @PathVariable UUID stageId,
            @Valid @RequestBody UpdateProjectStageRequest request) {
        return service.update(
                user.tenantId(),
                projectId,
                stageId,
                user.userId(),
                user.role(),
                request);
    }

    @DeleteMapping("/{stageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID projectId,
            @PathVariable UUID stageId) {
        service.delete(
                user.tenantId(),
                projectId,
                stageId,
                user.userId(),
                user.role());
    }
}
