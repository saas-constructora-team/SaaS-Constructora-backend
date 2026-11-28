package com.SaaS.Constructora.identity.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad de dominio pura (sin dependencias de JPA).
 * Representa una empresa/constructora cliente del SaaS (HU-01).
 */
public class Tenant {

    private final UUID id;
    private final String name;
    private final String slug;
    private final String tradeName;
    private final String taxId;
    private final String contactPhone;
    private final String address;
    private final String companyEmail;
    private final UUID ownerUserId;
    private final LocalDateTime createdAt;

    /** Alta básica (p. ej. registro con Google, donde solo se conoce el nombre). */
    public Tenant(UUID id, String name, String slug, LocalDateTime createdAt) {
        this(id, name, slug, null, null, null, null, null, null, createdAt);
    }

    public Tenant(UUID id, String name, String slug, String tradeName, String taxId,
                  String contactPhone, String address, String companyEmail,
                  LocalDateTime createdAt) {
        this(id, name, slug, tradeName, taxId, contactPhone, address, companyEmail, null, createdAt);
    }

    public Tenant(UUID id, String name, String slug, String tradeName, String taxId,
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

    /**
     * Regla de negocio: el slug se deriva del nombre.
     * Minúsculas, sin acentos, espacios y símbolos colapsados a guiones.
     */
    public static String slugify(String name) {
        String normalized = java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    public Tenant updateCompanyProfile(String name, String tradeName, String taxId,
                                       String contactPhone, String address, String companyEmail) {
        return new Tenant(id, name, slug, tradeName, taxId, contactPhone, address,
                companyEmail, ownerUserId, createdAt);
    }

    public boolean isOwnedBy(UUID userId) {
        return ownerUserId != null && ownerUserId.equals(userId);
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getTradeName() { return tradeName; }
    public String getTaxId() { return taxId; }
    public String getContactPhone() { return contactPhone; }
    public String getAddress() { return address; }
    public String getCompanyEmail() { return companyEmail; }
    public UUID getOwnerUserId() { return ownerUserId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
