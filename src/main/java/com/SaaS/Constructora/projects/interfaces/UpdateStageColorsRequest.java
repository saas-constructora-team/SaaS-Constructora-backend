package com.SaaS.Constructora.projects.interfaces;

import com.SaaS.Constructora.projects.domain.StageColor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/**
 * Cambio atómico del color de varias etapas. El tablero agrupa por nombre las
 * etapas de distintos proyectos, así que una sola selección puede afectar a
 * varias de ellas y debe persistirse completa o no persistirse.
 */
public record UpdateStageColorsRequest(
        @NotEmpty(message = "Se requiere al menos una etapa")
        List<@Valid StageColorUpdate> stages) {

    public record StageColorUpdate(
            @NotNull(message = "El proyecto es obligatorio") UUID projectId,
            @NotNull(message = "La etapa es obligatoria") UUID stageId,
            @NotNull(message = "El color es obligatorio") StageColor color) { }
}
