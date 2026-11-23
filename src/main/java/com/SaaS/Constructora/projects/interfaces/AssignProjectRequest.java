package com.SaaS.Constructora.projects.interfaces;
import com.SaaS.Constructora.projects.domain.ProjectRole;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Alta de un usuario en un proyecto. El rol es opcional y, si no se envía,
 * el usuario queda como {@link ProjectRole#MEMBER}. Solo un ADMIN puede
 * enviar un rol distinto de MEMBER para delegar la gestión del proyecto.
 */
public record AssignProjectRequest(
    @NotNull(message = "El usuario es obligatorio") UUID userId,
    ProjectRole role
) {
    public ProjectRole roleOrDefault() {
        return role != null ? role : ProjectRole.MEMBER;
    }
}
