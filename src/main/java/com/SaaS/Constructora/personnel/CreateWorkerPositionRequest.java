package com.SaaS.Constructora.personnel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para registrar un nuevo cargo dentro de la empresa.
 */
public record CreateWorkerPositionRequest(

        @NotBlank(message = "El nombre del cargo es obligatorio")
        @Size(max = 50) String name,

        @Size(max = 255) String description

) { }
