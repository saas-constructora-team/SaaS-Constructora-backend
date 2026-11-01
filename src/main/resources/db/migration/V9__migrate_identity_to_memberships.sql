-- Alinea bases de datos creadas con el diseño anterior de identity
-- (users.tenant_id y users.role) con el modelo actual basado en memberships.
-- En bases nuevas V3 ya crea memberships; por eso todas las operaciones son
-- idempotentes y esta migración solo actúa sobre el esquema heredado.

CREATE TABLE IF NOT EXISTS memberships (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tenant_id   UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    role        VARCHAR(30) NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, tenant_id, role)
);

CREATE INDEX IF NOT EXISTS idx_memberships_user ON memberships(user_id);
CREATE INDEX IF NOT EXISTS idx_memberships_tenant ON memberships(tenant_id);
CREATE INDEX IF NOT EXISTS idx_memberships_active ON memberships(user_id, tenant_id, active);

-- Migra el rol/tenant que antes vivía en users hacia memberships y elimina
-- esas columnas para que el modelo actual (users + memberships) sea válido.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'users'
          AND column_name = 'tenant_id'
    ) THEN
        INSERT INTO memberships (id, user_id, tenant_id, role, active, created_at)
        SELECT gen_random_uuid(), u.id, u.tenant_id, u.role, u.active, u.created_at
        FROM users u
        WHERE NOT EXISTS (
            SELECT 1
            FROM memberships m
            WHERE m.user_id = u.id
              AND m.tenant_id = u.tenant_id
              AND m.role = u.role
        );

        DROP INDEX IF EXISTS idx_users_tenant;
        ALTER TABLE users DROP COLUMN tenant_id;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'users'
          AND column_name = 'role'
    ) THEN
        ALTER TABLE users DROP COLUMN role;
    END IF;
END $$;
