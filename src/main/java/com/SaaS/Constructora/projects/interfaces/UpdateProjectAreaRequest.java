package com.SaaS.Constructora.projects.interfaces;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProjectAreaRequest(
        @NotBlank(message = "El nombre del área es obligatorio")
        @Size(max = 150, message = "El nombre del área no puede superar 150 caracteres")
        String name
) { }
