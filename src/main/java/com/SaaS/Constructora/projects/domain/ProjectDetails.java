package com.SaaS.Constructora.projects.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos de captación (intake) de un proyecto. Se usa tanto al crear como al
 * editar, y mantiene separado el DTO web de la entidad JPA.
 */
public record ProjectDetails(
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
        boolean hasSitePhotos
) { }
