-- Módulo identity (HU-01): Tenants, Usuarios y Roles
CREATE TABLE tenants (
    id         UUID PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    slug       VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE users (
    id            UUID PRIMARY KEY,
    tenant_id     UUID NOT NULL REFERENCES tenants(id),
    email         VARCHAR(255) NOT NULL UNIQUE,
    full_name     VARCHAR(150) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(30) NOT NULL,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_tenant ON users(tenant_id);

-- Usuario inicial para poder probar el login (HU-01)
-- Credenciales: admin@demo.com / Admin123!
INSERT INTO tenants (id, name, slug)
VALUES ('5f2b9c1d-3e47-4a86-9c21-8d4f0a7b5e13', 'Constructora Demo', 'demo');

INSERT INTO users (id, tenant_id, email, full_name, password_hash, role)
VALUES ('7c4e1f9a-2b58-4d3e-8a17-6f0c9d2e4b81',
        '5f2b9c1d-3e47-4a86-9c21-8d4f0a7b5e13',
        'admin@demo.com',
        'Administrador Demo',
        '$2a$10$6F6BWX81C4/SfTLwns8/QuIL0pDCmhhGQ/U41MShn169Du/48amdm',
        'ADMIN');
