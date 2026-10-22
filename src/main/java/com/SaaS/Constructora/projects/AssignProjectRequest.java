package com.SaaS.Constructora.projects;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AssignProjectRequest(
    @NotNull(message = "El usuario es obligatorio") UUID userId
) { }
