-- Módulo identity (HU-01): Tenants, Usuarios y Roles
CREATE TABLE tenants (
    id         UUID PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    slug       VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE users (
    id            UUID PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    full_name     VARCHAR(150) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE memberships (
    id          UUID PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tenant_id   UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    role        VARCHAR(30) NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, tenant_id, role)
);

CREATE INDEX idx_memberships_user ON memberships(user_id);
CREATE INDEX idx_memberships_tenant ON memberships(tenant_id);
CREATE INDEX idx_memberships_active ON memberships(user_id, tenant_id, active);

-- Usuario inicial para poder probar el login (HU-01)
-- Credenciales: admin@demo.com / Admin123!
INSERT INTO tenants (id, name, slug)
VALUES ('5f2b9c1d-3e47-4a86-9c21-8d4f0a7b5e13', 'Constructora Demo', 'demo');

INSERT INTO users (id, email, full_name, password_hash)
VALUES ('7c4e1f9a-2b58-4d3e-8a17-6f0c9d2e4b81',
        'admin@demo.com',
        'Administrador Demo',
        '$2a$10$6F6BWX81C4/SfTLwns8/QuIL0pDCmhhGQ/U41MShn169Du/48amdm');

INSERT INTO memberships (id, user_id, tenant_id, role)
VALUES ('a1b2c3d4-e5f6-7890-abcd-ef1234567890',
        '7c4e1f9a-2b58-4d3e-8a17-6f0c9d2e4b81',
        '5f2b9c1d-3e47-4a86-9c21-8d4f0a7b5e13',
        'ADMIN');
