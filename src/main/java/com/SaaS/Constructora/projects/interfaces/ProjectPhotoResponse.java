package com.SaaS.Constructora.projects.interfaces;

import com.SaaS.Constructora.projects.domain.ProjectPhoto;

import java.time.LocalDateTime;
import java.util.UUID;

/** Metadatos de evidencia fotográfica. El binario no se expone: vive en Object Storage. */
public record ProjectPhotoResponse(
        UUID id,
        UUID projectId,
        UUID stageId,
        String objectKey,
        String contentType,
        long sizeBytes,
        String sha256,
        String originalName,
        UUID uploadedBy,
        LocalDateTime createdAt
) {
    public static ProjectPhotoResponse from(ProjectPhoto photo) {
        return new ProjectPhotoResponse(
                photo.id(), photo.projectId(), photo.stageId(), photo.objectKey(),
                photo.contentType(), photo.sizeBytes(), photo.sha256(), photo.originalName(),
                photo.uploadedBy(), photo.createdAt());
    }
}
