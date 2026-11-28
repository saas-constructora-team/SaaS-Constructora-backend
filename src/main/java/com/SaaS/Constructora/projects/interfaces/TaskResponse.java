package com.SaaS.Constructora.projects.interfaces;
import com.SaaS.Constructora.projects.domain.AreaTipo;
import com.SaaS.Constructora.projects.domain.ProjectAssignment;
import com.SaaS.Constructora.projects.infrastructure.ProjectArea;
import com.SaaS.Constructora.projects.infrastructure.Task;
import com.SaaS.Constructora.projects.infrastructure.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record TaskResponse(
        UUID id,
        UUID projectId,
        String title,
        String description,
        StatusResponse status,
        AreaResponse area,
        AssignedUser assignee,
        AssignedUser validator,
        LocalDate fechaFinPrevista,
        AreaDetailsResponse areaDetails,
        UUID createdBy,
        LocalDateTime createdAt,
        UUID stageId
) {
    public record StatusResponse(UUID id, String name, String color, int order,
                                 boolean isDefault, boolean isSystem) { }

    public record AreaResponse(UUID id, String name, AreaTipo tipo) { }

    public record AssignedUser(UUID assignmentId, UUID userId) { }

    public static TaskResponse from(Task task, TaskStatus status, ProjectArea area,
                              ProjectAssignment assignee, ProjectAssignment validator) {
        AreaTipo tipo = area == null ? null : area.getTipo();
        return new TaskResponse(
                task.getId(),
                task.getProjectId(),
                task.getTitle(),
                task.getDescription(),
                new StatusResponse(status.getId(), status.getName(), status.getColor(),
                        status.getStatusOrder(), status.isDefaultStatus(), status.isSystemStatus()),
                area == null ? null : new AreaResponse(area.getId(), area.getName(), tipo),
                assignee == null ? null : new AssignedUser(assignee.getId(), assignee.getUserId()),
                validator == null ? null : new AssignedUser(validator.getId(), validator.getUserId()),
                task.getFechaFinPrevista(),
                tipo == null || tipo == AreaTipo.PERSONALIZADA || task.getDetallesArea() == null
                        ? null
                        : AreaDetailsResponse.from(task.getDetallesArea()),
                task.getCreatedBy(),
                task.getCreatedAt(),
                task.getStageId());
    }
}
