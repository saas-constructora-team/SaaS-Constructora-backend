package com.SaaS.Constructora.sitelog.interfaces;

import com.SaaS.Constructora.sitelog.domain.SiteLogStatus;
import com.SaaS.Constructora.sitelog.domain.SiteLogType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

/** Cuerpo compartido por el alta y la edición; el proyecto va en la ruta. */
public record SiteLogRecordRequest(
        @NotNull SiteLogType type,
        @NotNull SiteLogStatus status,
        @NotNull LocalDateTime when,
        @NotBlank @Size(max = 300) String where,
        @NotBlank @Size(max = 500) String what,
        @NotBlank String how,
        String why,
        UUID assigneeId
) { }
