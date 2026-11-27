package com.SaaS.Constructora.sitelog.interfaces;

import com.SaaS.Constructora.sitelog.domain.SiteLogRecord;
import com.SaaS.Constructora.sitelog.domain.SiteLogStatus;
import com.SaaS.Constructora.sitelog.domain.SiteLogType;

import java.time.LocalDateTime;
import java.util.UUID;

/** Contrato de respuesta: coincide con `SiteLogEntry` del frontend. */
public record SiteLogRecordResponse(
        UUID id,
        UUID projectId,
        String projectName,
        SiteLogType type,
        SiteLogStatus status,
        String what,
        String how,
        LocalDateTime when,
        String where,
        String why,
        String author,
        UUID assigneeId,
        String assigneeName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        int photos
) {
    public static SiteLogRecordResponse from(SiteLogRecord record, String projectName,
                                             String authorName, String assigneeName) {
        return new SiteLogRecordResponse(
                record.id(), record.projectId(), projectName, record.type(), record.status(),
                record.what(), record.how(), record.eventAt(), record.location(), record.why(),
                authorName, record.assigneeId(), assigneeName,
                record.createdAt(), record.updatedAt(), 0);
    }
}
