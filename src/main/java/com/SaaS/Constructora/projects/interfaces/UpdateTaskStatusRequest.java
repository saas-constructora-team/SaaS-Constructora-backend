package com.SaaS.Constructora.projects.interfaces;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateTaskStatusRequest(
        @NotBlank(message = "El nombre del estado es obligatorio")
        @Size(max = 80, message = "El nombre del estado no puede superar 80 caracteres")
        String name,

        @NotBlank(message = "El color del estado es obligatorio")
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "El color debe ser hexadecimal, por ejemplo #22C55E")
        String color,

        Boolean isDefault
) { }
