package com.SaaS.Constructora.personnel;

import java.time.LocalDateTime;
import java.util.UUID;

public record WorkerResponse(
        UUID id,
        String fullName,
        String documentType,
        String documentNumber,
        String phone,
        String email,
        String emergencyContactName,
        String emergencyContactPhone,
        String position,
        UUID projectId,
        boolean active,
        LocalDateTime createdAt
) {
    public static WorkerResponse from(Worker worker) {
        return new WorkerResponse(
                worker.getId(),
                worker.getFullName(),
                worker.getDocumentType().name(),
                worker.getDocumentNumber(),
                worker.getPhone(),
                worker.getEmail(),
                worker.getEmergencyContactName(),
                worker.getEmergencyContactPhone(),
                worker.getPositionName(),
                worker.getProjectId(),
                worker.isActive(),
                worker.getCreatedAt());
    }
}
