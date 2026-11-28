-- Cuenta/perfil: username global por usuario y owner explícito por tenant.
ALTER TABLE users ADD COLUMN username VARCHAR(30);

CREATE UNIQUE INDEX uq_users_username_lower
    ON users (LOWER(username))
    WHERE username IS NOT NULL;

ALTER TABLE tenants ADD COLUMN owner_user_id UUID;

-- Backfill determinista: primer ADMIN del tenant por antigüedad de membresía.
UPDATE tenants t
SET owner_user_id = sub.user_id
FROM (
    SELECT DISTINCT ON (m.tenant_id) m.tenant_id, m.user_id
    FROM memberships m
    WHERE m.role = 'ADMIN'
    ORDER BY m.tenant_id, m.created_at ASC, m.id ASC
) sub
WHERE t.id = sub.tenant_id
  AND t.owner_user_id IS NULL;

-- Fallback defensivo para datos legacy sin ADMIN: primera membresía activa.
UPDATE tenants t
SET owner_user_id = sub.user_id
FROM (
    SELECT DISTINCT ON (m.tenant_id) m.tenant_id, m.user_id
    FROM memberships m
    WHERE m.active = TRUE
    ORDER BY m.tenant_id, m.created_at ASC, m.id ASC
) sub
WHERE t.id = sub.tenant_id
  AND t.owner_user_id IS NULL;

ALTER TABLE tenants ALTER COLUMN owner_user_id SET NOT NULL;
ALTER TABLE tenants
    ADD CONSTRAINT fk_tenants_owner_user FOREIGN KEY (owner_user_id) REFERENCES users(id);
CREATE INDEX idx_tenants_owner_user_id ON tenants(owner_user_id);
