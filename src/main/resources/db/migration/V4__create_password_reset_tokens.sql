-- Módulo identity (HU-13): tokens de recuperación de contraseña
-- Se guarda únicamente el HASH del token, nunca el valor en claro.
CREATE TABLE password_reset_tokens (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users(id),
    token_hash VARCHAR(100) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_password_reset_user ON password_reset_tokens(user_id);
CREATE INDEX idx_password_reset_hash ON password_reset_tokens(token_hash);
