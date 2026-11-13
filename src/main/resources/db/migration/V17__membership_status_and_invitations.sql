-- Invitaciones de trabajadores (invite-and-claim) y estado de membresía.
--
-- 1) memberships.status distingue una membresía pendiente (INVITED) de una
--    operativa (ACTIVE). active se conserva como interruptor de habilitación.
-- 2) Se garantiza una sola membresía por (user_id, tenant_id): antes el único
--    era (user_id, tenant_id, role).
-- 3) membership_invitations guarda solo el hash del token; el token en claro
--    viaja únicamente en el correo.

ALTER TABLE memberships
    ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'memberships'::regclass
          AND conname = 'chk_memberships_status'
    ) THEN
        ALTER TABLE memberships
            ADD CONSTRAINT chk_memberships_status
            CHECK (status IN ('INVITED', 'ACTIVE'));
    END IF;
END $$;

-- Retira el constraint antiguo UNIQUE (user_id, tenant_id, role), sea cual sea
-- el nombre que PostgreSQL le haya asignado.
DO $$
DECLARE
    constraint_name RECORD;
BEGIN
    FOR constraint_name IN
        SELECT conname
        FROM pg_constraint
        WHERE conrelid = 'memberships'::regclass
          AND contype = 'u'
          AND pg_get_constraintdef(oid) LIKE '%user_id, tenant_id, role%'
    LOOP
        EXECUTE format('ALTER TABLE memberships DROP CONSTRAINT %I', constraint_name.conname);
    END LOOP;
END $$;

-- Conserva una sola membresía por usuario/tenant. Si había varias, se prefiere
-- ADMIN y, entre iguales, la más reciente.
DELETE FROM memberships m
USING (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY user_id, tenant_id
               ORDER BY (role = 'ADMIN') DESC, created_at DESC, id
           ) AS row_number
    FROM memberships
) duplicates
WHERE m.id = duplicates.id
  AND duplicates.row_number > 1;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'memberships'::regclass
          AND conname = 'uk_memberships_user_tenant'
    ) THEN
        ALTER TABLE memberships
            ADD CONSTRAINT uk_memberships_user_tenant
            UNIQUE (user_id, tenant_id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_memberships_tenant_status
    ON memberships(tenant_id, status);

CREATE TABLE IF NOT EXISTS membership_invitations (
    id                  UUID PRIMARY KEY,
    membership_id       UUID NOT NULL REFERENCES memberships(id) ON DELETE CASCADE,
    tenant_id           UUID NOT NULL,
    email               VARCHAR(255) NOT NULL,
    invited_by_user_id  UUID NOT NULL REFERENCES users(id),
    token_hash          VARCHAR(100) NOT NULL UNIQUE,
    expires_at          TIMESTAMP NOT NULL,
    accepted_at         TIMESTAMP,
    used                BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_membership_invitations_email
    ON membership_invitations(email);
CREATE INDEX IF NOT EXISTS idx_membership_invitations_tenant
    ON membership_invitations(tenant_id);
CREATE INDEX IF NOT EXISTS idx_membership_invitations_membership
    ON membership_invitations(membership_id);
CREATE INDEX IF NOT EXISTS idx_membership_invitations_expires
    ON membership_invitations(expires_at);

-- El flujo nuevo no escribe la contraseña en personnel; la fuente única es
-- users.password_hash. La columna queda nullable para workers históricos.
ALTER TABLE workers
    ALTER COLUMN password_hash DROP NOT NULL;
