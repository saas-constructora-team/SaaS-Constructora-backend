package com.SaaS.Constructora.projects.interfaces;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Etapa que se quiere dejar como en curso. */
public record UpdateProjectProgressRequest(@NotNull(message = "La etapa es obligatoria") UUID stageId) {
}
