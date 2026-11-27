package com.SaaS.Constructora.sitelog.interfaces;

import com.SaaS.Constructora.sitelog.domain.SiteLogEntry;

import java.time.LocalDateTime;
import java.util.UUID;

public record SiteLogEntryResponse(
        UUID id,
        UUID projectId,
        UUID stageId,
        UUID documentId,
        UUID createdBy,
        LocalDateTime createdAt
) {
    public static SiteLogEntryResponse from(SiteLogEntry entry) {
        return new SiteLogEntryResponse(entry.id(), entry.projectId(), entry.stageId(),
                entry.documentId(), entry.createdBy(), entry.createdAt());
    }
}
