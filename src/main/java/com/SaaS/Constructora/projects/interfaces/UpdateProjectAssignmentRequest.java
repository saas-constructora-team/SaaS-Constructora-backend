package com.SaaS.Constructora.projects.interfaces;
import com.SaaS.Constructora.projects.domain.ProjectRole;

import jakarta.validation.constraints.NotNull;

/**
 * Modificación del rol de un usuario dentro de un proyecto.
 * Solo un ADMIN puede nombrar o retirar el rol {@link ProjectRole#LEAD}.
 */
public record UpdateProjectAssignmentRequest(
    @NotNull(message = "El rol es obligatorio") ProjectRole role
) { }
