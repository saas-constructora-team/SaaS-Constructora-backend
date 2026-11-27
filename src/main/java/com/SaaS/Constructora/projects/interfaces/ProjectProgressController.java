package com.SaaS.Constructora.projects.interfaces;

import com.SaaS.Constructora.projects.application.ProjectProgressService;
import com.SaaS.Constructora.shared.security.AuthenticatedUser;
import com.SaaS.Constructora.shared.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Avance compartido de un proyecto: la etapa en curso ya no depende del navegador. */
@RestController
@RequestMapping("/api/projects/{projectId}/progress")
public class ProjectProgressController {

    private final ProjectProgressService service;

    ProjectProgressController(ProjectProgressService service) {
        this.service = service;
    }

    @GetMapping
    ProjectProgressResponse get(@CurrentUser AuthenticatedUser user,
                                @PathVariable UUID projectId) {
        return service.get(user.tenantId(), projectId, user.userId(), user.role());
    }

    @PutMapping
    ProjectProgressResponse select(@CurrentUser AuthenticatedUser user,
                                   @PathVariable UUID projectId,
                                   @Valid @RequestBody UpdateProjectProgressRequest request) {
        return service.select(user.tenantId(), projectId, user.userId(), user.role(), request);
    }
}
