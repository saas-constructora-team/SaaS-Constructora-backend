package com.SaaS.Constructora.projects;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProjectStageRequest(
        @NotBlank(message = "El nombre de la etapa es obligatorio")
        @Size(max = 150, message = "El nombre de la etapa no puede superar 150 caracteres")
        String name
) { }
