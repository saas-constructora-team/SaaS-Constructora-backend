package com.SaaS.Constructora.personnel;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Trabajador (personal) de una empresa constructora.
 * <p>
 * Incluye sus datos personales, su cargo dentro de la empresa y, opcionalmente,
 * la obra a la que está vinculado. El tenant se referencia por ID (nunca con
 * {@code @ManyToOne}) y toda consulta debe filtrarlo explícitamente.
 */
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Entity
@Table(name = "workers", uniqueConstraints = @UniqueConstraint(
        name = "uk_workers_tenant_document",
        columnNames = {"tenant_id", "document_type", "document_number"}))
public class Worker {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    private DocumentType documentType;

    @Column(name = "document_number", nullable = false, length = 50)
    private String documentNumber;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(length = 255)
    private String email;

    /* Contraseña del trabajador: hash BCrypt que permite el inicio de
     * sesión de la cuenta del personal. Se almacena hasheado, nunca en texto
     * plano. En el flujo de invitación queda NULL: la contraseña vive
     * únicamente en users.password_hash. */
    @Column(name = "password_hash", nullable = true, length = 100)
    private String passwordHash;

    @Column(name = "user_id", nullable = true, updatable = false, length = 36)
    private UUID userId;

    @Column(name = "emergency_contact_name", nullable = false, length = 150)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", nullable = false, length = 30)
    private String emergencyContactPhone;

    /* Cargo del trabajador: nombre del cargo (puede ser uno de los cargos por defecto
     * o un cargo creado dinámicamente). Se almacena como texto para permitir
     * posiciones dinámicas sin necesidad de migrar la base de datos. */
    @Column(name = "job_position", nullable = false, length = 40)
    private String positionName;

    /* Referencia por ID al proyecto/obra (módulo projects). NO usar @ManyToOne. */
    @Column(name = "project_id")
    private UUID projectId;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Worker() { } // requerido por JPA

    public Worker(UUID tenantId, String fullName, DocumentType documentType, String documentNumber,
                  String phone, String email, String passwordHash, UUID userId,
                  String emergencyContactName, String emergencyContactPhone, String positionName,
                  UUID projectId) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.fullName = fullName;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.phone = phone;
        this.email = email;
        this.passwordHash = passwordHash;
        this.userId = userId;
        this.emergencyContactName = emergencyContactName;
        this.emergencyContactPhone = emergencyContactPhone;
        this.positionName = positionName;
        this.projectId = projectId;
        this.active = true;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }

    /** Vincula un worker preexistente con la identidad que acaba de aceptar la invitación. */
    public void linkToUser(UUID userId) {
        this.userId = userId;
    }

    public UUID getTenantId() { return tenantId; }
    public String getFullName() { return fullName; }
    public DocumentType getDocumentType() { return documentType; }
    public String getDocumentNumber() { return documentNumber; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getEmergencyContactName() { return emergencyContactName; }
    public String getEmergencyContactPhone() { return emergencyContactPhone; }

    public String getPositionName() { return positionName; }
    public UUID getProjectId() { return projectId; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
