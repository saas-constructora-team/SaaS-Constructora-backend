package com.SaaS.Constructora.personnel.interfaces;
import com.SaaS.Constructora.personnel.infrastructure.WorkerPosition;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representa un cargo dentro de la empresa constructora.
 */
public record WorkerPositionResponse(
        UUID id,
        UUID tenantId,
        String name,
        String description,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static WorkerPositionResponse from(WorkerPosition position) {
        return new WorkerPositionResponse(
                position.getId(),
                position.getTenantId(),
                position.getName(),
                position.getDescription(),
                position.isActive(),
                position.getCreatedAt(),
                position.getUpdatedAt()
        );
    }
}
