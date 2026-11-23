package com.SaaS.Constructora.personnel.interfaces;

import jakarta.validation.constraints.Size;

/**
 * Datos para actualizar un cargo existente.
 * Todos los campos son opcionales: solo se actualizan los que se envían.
 */
public record UpdateWorkerPositionRequest(

        @Size(max = 50) String name,

        @Size(max = 255) String description,

        Boolean active

) { }
