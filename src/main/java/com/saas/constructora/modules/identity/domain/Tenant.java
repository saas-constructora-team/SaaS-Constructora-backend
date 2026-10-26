package com.saas.constructora.modules.identity.domain;

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
    private final LocalDateTime createdAt;

    public Tenant(UUID id, String name, String slug, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.slug = slug;
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

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
