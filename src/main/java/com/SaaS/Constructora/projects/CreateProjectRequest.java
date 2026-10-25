package com.SaaS.Constructora.projects;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateProjectRequest(
        @NotBlank(message = "El nombre del proyecto es obligatorio")
        @Size(max = 200) String name,
        @Size(max = 200) String clientName,
        @Size(max = 300) String location,
        @Email(message = "El correo electrónico no es válido")
        @Size(max = 200) String contactEmail,
        @Size(max = 50) String contactPhone,
        ProjectType projectType,
        ProjectType propertyUse,
        @DecimalMin(value = "0.0", message = "El área no puede ser negativa")
        @Digits(integer = 10, fraction = 2) BigDecimal estimatedArea,
        @Min(value = 0, message = "El número de usuarios no puede ser negativo")
        Integer occupantCount,
        @Size(max = 1000) String requiredSpaces,
        @Size(max = 2000) String styleReferences,
        BudgetRange budgetRange,
        LocalDate estimatedStartDate,
        LocalDate estimatedDeliveryDate,
        LotOwnership lotOwnership,
        Boolean hasDeeds,
        Boolean hasTopographicSurvey,
        Boolean hasPreviousPlans,
        Boolean hasSitePhotos
) {
    /** Constructor de compatibilidad para los campos originales. */
    public CreateProjectRequest(String name, String clientName, String location) {
        this(name, clientName, location, null, null, null, null, null, null,
                null, null, null, null, null, null, false, false, false, false);
    }

    ProjectDetails toDetails() {
        return new ProjectDetails(name, clientName, location, contactEmail, contactPhone,
                projectType, propertyUse, estimatedArea, occupantCount, requiredSpaces,
                styleReferences, budgetRange, estimatedStartDate, estimatedDeliveryDate,
                lotOwnership, Boolean.TRUE.equals(hasDeeds), Boolean.TRUE.equals(hasTopographicSurvey),
                Boolean.TRUE.equals(hasPreviousPlans), Boolean.TRUE.equals(hasSitePhotos));
    }
}
