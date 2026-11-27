package com.SaaS.Constructora.documents.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Contenido de una versión de documento. El binario vive en Object Storage y
 * aquí solo se guarda la clave del objeto, nunca los bytes.
 *
 * El modo append-only mantiene la versión inmutable. La sobrescritura
 * reemplaza el contenido de la versión vigente conservando su identidad y su
 * fecha de creación original.
 */
public record DocumentVersion(
        UUID id,
        UUID tenantId,
        UUID documentId,
        String versionLabel,
        String objectKey,
        String contentType,
        long sizeBytes,
        String sha256,
        String originalName,
        UUID uploadedBy,
        LocalDateTime createdAt,
        LocalDateTime changedAt,
        boolean binaryAvailable
) {

    /** Versión recién almacenada: coincide su creación con su último cambio. */
    public static DocumentVersion stored(UUID id, UUID tenantId, UUID documentId, String versionLabel,
                                         String objectKey, String contentType, long sizeBytes,
                                         String sha256, String originalName, UUID uploadedBy,
                                         LocalDateTime createdAt) {
        return new DocumentVersion(id, tenantId, documentId, versionLabel, objectKey, contentType,
                sizeBytes, sha256, originalName, uploadedBy, createdAt, createdAt, true);
    }

    /** La metadata se conserva en el historial aunque su binario haya sido podado. */
    public DocumentVersion withoutBinary() {
        return new DocumentVersion(id, tenantId, documentId, versionLabel, objectKey, contentType,
                sizeBytes, sha256, originalName, uploadedBy, createdAt, changedAt, false);
    }
}
