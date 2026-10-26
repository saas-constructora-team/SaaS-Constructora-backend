package com.saas.constructora.modules.projects.interfaces.dto;

import com.saas.constructora.modules.projects.domain.ProjectStatus;
import com.saas.constructora.modules.projects.domain.ProjectType;
import com.saas.constructora.modules.projects.domain.BudgetRange;
import com.saas.constructora.modules.projects.domain.LotOwnership;
import com.saas.constructora.modules.projects.infrastructure.Project;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        UUID tenantId,
        String name,
        String clientName,
        String location,
        String contactEmail,
        String contactPhone,
        ProjectType projectType,
        ProjectType propertyUse,
        BigDecimal estimatedArea,
        Integer occupantCount,
        String requiredSpaces,
        String styleReferences,
        BudgetRange budgetRange,
        LocalDate estimatedStartDate,
        LocalDate estimatedDeliveryDate,
        LotOwnership lotOwnership,
        boolean hasDeeds,
        boolean hasTopographicSurvey,
        boolean hasPreviousPlans,
        boolean hasSitePhotos,
        ProjectStatus status,
        LocalDateTime createdAt
) {
    public static ProjectResponse from(Project p) {
        return new ProjectResponse(
                p.getId(), p.getTenantId(), p.getName(), p.getClientName(), p.getLocation(),
                p.getContactEmail(), p.getContactPhone(), p.getProjectType(), p.getPropertyUse(),
                p.getEstimatedArea(), p.getOccupantCount(), p.getRequiredSpaces(),
                p.getStyleReferences(), p.getBudgetRange(), p.getEstimatedStartDate(),
                p.getEstimatedDeliveryDate(), p.getLotOwnership(), p.isHasDeeds(),
                p.isHasTopographicSurvey(), p.isHasPreviousPlans(), p.isHasSitePhotos(),
                p.getStatus(), p.getCreatedAt());
    }
}
