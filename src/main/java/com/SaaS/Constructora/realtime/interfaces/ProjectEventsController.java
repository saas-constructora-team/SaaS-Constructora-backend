package com.SaaS.Constructora.realtime.interfaces;

import com.SaaS.Constructora.projects.application.ports.ProjectTenantPort;
import com.SaaS.Constructora.realtime.infrastructure.RealtimeSubscriptionRegistry;
import com.SaaS.Constructora.shared.security.AuthenticatedUser;
import com.SaaS.Constructora.shared.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/**
 * Flujo SSE de cambios de proyectos. El tenant se toma del JWT; si se pide un
 * proyecto concreto se valida que pertenezca al tenant y que el usuario pueda
 * verlo. No se envían datos de negocio, solo el aviso de que algo cambió.
 */
@RestController
@RequestMapping("/api/projects/events")
public class ProjectEventsController {

    private final RealtimeSubscriptionRegistry registry;
    private final ProjectTenantPort projects;

    public ProjectEventsController(RealtimeSubscriptionRegistry registry,
                                   ProjectTenantPort projects) {
        this.registry = registry;
        this.projects = projects;
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter stream(@CurrentUser AuthenticatedUser user,
                      @RequestParam(name = "projectId", required = false) UUID projectId) {
        if (projectId != null) {
            projects.ensureBelongsToTenant(projectId, user.tenantId());
            if (!user.role().canViewAllProjects()
                    && !projects.isUserAssignedToProject(projectId, user.tenantId(), user.userId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Proyecto no encontrado");
            }
        }
        return registry.subscribe(user.tenantId(), projectId);
    }
}
