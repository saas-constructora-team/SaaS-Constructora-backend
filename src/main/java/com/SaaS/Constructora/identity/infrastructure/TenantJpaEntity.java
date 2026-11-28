package com.SaaS.Constructora.identity.infrastructure;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenants")
class TenantJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(name = "trade_name", length = 150)
    private String tradeName;

    @Column(name = "tax_id", length = 50)
    private String taxId;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Column(length = 255)
    private String address;

    @Column(name = "company_email", length = 255)
    private String companyEmail;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected TenantJpaEntity() { } // requerido por JPA

    TenantJpaEntity(UUID id, String name, String slug, String tradeName, String taxId,
                    String contactPhone, String address, String companyEmail,
                    UUID ownerUserId, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.slug = slug;
        this.tradeName = tradeName;
        this.taxId = taxId;
        this.contactPhone = contactPhone;
        this.address = address;
        this.companyEmail = companyEmail;
        this.ownerUserId = ownerUserId;
        this.createdAt = createdAt;
    }

    UUID getId() { return id; }
    String getName() { return name; }
    String getSlug() { return slug; }
    String getTradeName() { return tradeName; }
    String getTaxId() { return taxId; }
    String getContactPhone() { return contactPhone; }
    String getAddress() { return address; }
    String getCompanyEmail() { return companyEmail; }
    UUID getOwnerUserId() { return ownerUserId; }
    LocalDateTime getCreatedAt() { return createdAt; }

    void updateCompanyProfile(String name, String tradeName, String taxId,
                              String contactPhone, String address, String companyEmail) {
        this.name = name;
        this.tradeName = tradeName;
        this.taxId = taxId;
        this.contactPhone = contactPhone;
        this.address = address;
        this.companyEmail = companyEmail;
    }
}
