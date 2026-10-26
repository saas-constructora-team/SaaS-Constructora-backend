package com.saas.constructora.modules.identity.domain;

import java.util.UUID;

/**
 * Identidad federada vinculada a un usuario (HU-01).
 * Permite iniciar sesión con un proveedor externo, hoy Google.
 * El {@code providerUserId} es el "sub" del proveedor y es único por proveedor.
 */
public class ExternalIdentity {

    public static final String GOOGLE = "GOOGLE";

    private final UUID id;
    private final UUID userId;
    private final String provider;
    private final String providerUserId;
    private final java.time.LocalDateTime createdAt;

    public ExternalIdentity(UUID id, UUID userId, String provider,
                            String providerUserId, java.time.LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.createdAt = createdAt;
    }

    public static ExternalIdentity google(UUID userId, String googleSubject, java.time.LocalDateTime createdAt) {
        return new ExternalIdentity(UUID.randomUUID(), userId, GOOGLE, googleSubject, createdAt);
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getProvider() { return provider; }
    public String getProviderUserId() { return providerUserId; }
    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
}
