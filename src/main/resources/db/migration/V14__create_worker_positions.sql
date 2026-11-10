-- Tabla de cargos de trabajadores (positions) — permite posiciones dinámicas por tenant
-- No hay FK a tenants: el tenant se valida en la capa de aplicación.
CREATE TABLE worker_positions (
    id                      UUID PRIMARY KEY,
    tenant_id               UUID NOT NULL,
    name                    VARCHAR(50) NOT NULL,
    description             VARCHAR(255),
    active                  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP,

    CONSTRAINT uk_worker_positions_tenant_name
        UNIQUE (tenant_id, name)
);

CREATE INDEX idx_worker_positions_tenant ON worker_positions(tenant_id);
