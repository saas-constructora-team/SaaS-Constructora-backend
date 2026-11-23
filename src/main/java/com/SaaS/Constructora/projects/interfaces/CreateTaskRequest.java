package com.SaaS.Constructora.projects.interfaces;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateTaskRequest(
        @NotBlank(message = "El título de la tarea es obligatorio")
        @Size(max = 150, message = "El título de la tarea no puede superar 150 caracteres")
        String title,

        @Size(max = 2000, message = "La descripción no puede superar 2000 caracteres")
        String description,

        UUID statusId,
        UUID areaId,
        UUID assigneeUserId,
        UUID validatorUserId,
        LocalDate fechaFinPrevista,

        @Valid
        AreaDetailsRequest areaDetails
) {
    public CreateTaskRequest(String title, String description) {
        this(title, description, null, null, null, null, null, null);
    }

    public CreateTaskRequest(String title, String description, UUID statusId, UUID areaId,
                             UUID assigneeUserId, UUID validatorUserId) {
        this(title, description, statusId, areaId, assigneeUserId, validatorUserId, null, null);
    }
}
