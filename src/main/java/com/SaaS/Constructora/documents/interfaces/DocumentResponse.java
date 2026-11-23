package com.SaaS.Constructora.documents.interfaces;

import com.SaaS.Constructora.documents.domain.AssetKind;
import com.SaaS.Constructora.documents.domain.Document;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        UUID tenantId,
        UUID projectId,
        AssetKind kind,
        String title,
        UUID currentVersionId,
        LocalDateTime createdAt
) {
    public static DocumentResponse from(Document document) {
        return new DocumentResponse(document.getId(), document.getTenantId(),
                document.getProjectId(), document.getKind(), document.getTitle(),
                document.getCurrentVersionId(), document.getCreatedAt());
    }
}
