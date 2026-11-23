package com.SaaS.Constructora.projects.infrastructure;
import com.SaaS.Constructora.projects.domain.BudgetRange;
import com.SaaS.Constructora.projects.domain.LotOwnership;
import com.SaaS.Constructora.projects.domain.ProjectDetails;
import com.SaaS.Constructora.projects.domain.ProjectStatus;
import com.SaaS.Constructora.projects.domain.ProjectType;

import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Entity
@Table(name = "projects")
public class Project {

    @Id
    private UUID id;

    // Referencia por ID al tenant (módulo identity). NO usar @ManyToOne.
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "client_name", length = 200)
    private String clientName;

    @Column(length = 300)
    private String location;

    @Column(name = "contact_email", length = 200)
    private String contactEmail;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "project_type", length = 30)
    private ProjectType projectType;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_use", length = 30)
    private ProjectType propertyUse;

    @Column(name = "estimated_area", precision = 12, scale = 2)
    private BigDecimal estimatedArea;

    @Column(name = "occupant_count")
    private Integer occupantCount;

    @Column(name = "required_spaces", length = 1000)
    private String requiredSpaces;

    @Column(name = "style_references", length = 2000)
    private String styleReferences;

    @Enumerated(EnumType.STRING)
    @Column(name = "budget_range", length = 30)
    private BudgetRange budgetRange;

    @Column(name = "estimated_start_date")
    private LocalDate estimatedStartDate;

    @Column(name = "estimated_delivery_date")
    private LocalDate estimatedDeliveryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "lot_ownership", length = 30)
    private LotOwnership lotOwnership;

    @Column(name = "has_deeds", nullable = false)
    private boolean hasDeeds;

    @Column(name = "has_topographic_survey", nullable = false)
    private boolean hasTopographicSurvey;

    @Column(name = "has_previous_plans", nullable = false)
    private boolean hasPreviousPlans;

    @Column(name = "has_site_photos", nullable = false)
    private boolean hasSitePhotos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProjectStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Project() { } // requerido por JPA

    public Project(UUID tenantId, ProjectDetails details) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.status = ProjectStatus.ACTIVE; // criterio HU-02: nace "Activo"
        applyDetails(details);
    }

    /** Constructor mínimo conservado para pruebas y usos internos. */
    public Project(UUID tenantId, String name, String clientName, String location) {
        this(tenantId, new ProjectDetails(name, clientName, location,
                null, null, null, null, null, null, null, null, null, null, null, null,
                false, false, false, false));
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    /** Actualiza los datos editables del proyecto; el tenant y el estado no cambian. */
    public void updateDetails(ProjectDetails details) {
        applyDetails(details);
    }

    private void applyDetails(ProjectDetails details) {
        this.name = details.name();
        this.clientName = details.clientName();
        this.location = details.location();
        this.contactEmail = details.contactEmail();
        this.contactPhone = details.contactPhone();
        this.projectType = details.projectType();
        this.propertyUse = details.propertyUse();
        this.estimatedArea = details.estimatedArea();
        this.occupantCount = details.occupantCount();
        this.requiredSpaces = details.requiredSpaces();
        this.styleReferences = details.styleReferences();
        this.budgetRange = details.budgetRange();
        this.estimatedStartDate = details.estimatedStartDate();
        this.estimatedDeliveryDate = details.estimatedDeliveryDate();
        this.lotOwnership = details.lotOwnership();
        this.hasDeeds = details.hasDeeds();
        this.hasTopographicSurvey = details.hasTopographicSurvey();
        this.hasPreviousPlans = details.hasPreviousPlans();
        this.hasSitePhotos = details.hasSitePhotos();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getName() { return name; }
    public String getClientName() { return clientName; }
    public String getLocation() { return location; }
    public String getContactEmail() { return contactEmail; }
    public String getContactPhone() { return contactPhone; }
    public ProjectType getProjectType() { return projectType; }
    public ProjectType getPropertyUse() { return propertyUse; }
    public BigDecimal getEstimatedArea() { return estimatedArea; }
    public Integer getOccupantCount() { return occupantCount; }
    public String getRequiredSpaces() { return requiredSpaces; }
    public String getStyleReferences() { return styleReferences; }
    public BudgetRange getBudgetRange() { return budgetRange; }
    public LocalDate getEstimatedStartDate() { return estimatedStartDate; }
    public LocalDate getEstimatedDeliveryDate() { return estimatedDeliveryDate; }
    public LotOwnership getLotOwnership() { return lotOwnership; }
    public boolean isHasDeeds() { return hasDeeds; }
    public boolean isHasTopographicSurvey() { return hasTopographicSurvey; }
    public boolean isHasPreviousPlans() { return hasPreviousPlans; }
    public boolean isHasSitePhotos() { return hasSitePhotos; }
    public ProjectStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
