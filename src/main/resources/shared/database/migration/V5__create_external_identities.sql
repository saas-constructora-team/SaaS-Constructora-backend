-- Módulo identity (HU-01): identidades federadas (login con Google)
-- El provider_user_id es el "sub" del proveedor y es único por proveedor.
CREATE TABLE external_identities (
    id               UUID PRIMARY KEY,
    user_id          UUID NOT NULL REFERENCES users(id),
    provider         VARCHAR(30) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    created_at       TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_external_identity UNIQUE (provider, provider_user_id)
);

CREATE INDEX idx_external_identity_user ON external_identities(user_id);
