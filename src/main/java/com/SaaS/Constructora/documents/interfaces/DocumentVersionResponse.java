package com.SaaS.Constructora.documents.interfaces;

import com.SaaS.Constructora.documents.domain.DocumentVersion;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentVersionResponse(
        UUID id,
        UUID documentId,
        String versionLabel,
        String contentType,
        long sizeBytes,
        String sha256,
        String originalName,
        LocalDateTime createdAt
) {
    public static DocumentVersionResponse from(DocumentVersion version) {
        return new DocumentVersionResponse(version.id(), version.documentId(),
                version.versionLabel(), version.contentType(), version.sizeBytes(),
                version.sha256(), version.originalName(), version.createdAt());
    }
}
