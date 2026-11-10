-- Módulo personnel: trabajadores (personal) de cada empresa.
-- No hay FK a tenants ni a projects: el tenant se valida en la capa de
-- aplicación y la obra se referencia por ID sin acoplar los módulos.
CREATE TABLE workers (
    id                      UUID PRIMARY KEY,
    tenant_id               UUID NOT NULL,
    full_name               VARCHAR(150) NOT NULL,
    document_type           VARCHAR(30) NOT NULL,
    document_number         VARCHAR(50) NOT NULL,
    phone                   VARCHAR(30) NOT NULL,
    email                   VARCHAR(255),
    emergency_contact_name  VARCHAR(150) NOT NULL,
    emergency_contact_phone VARCHAR(30) NOT NULL,
    job_position            VARCHAR(40) NOT NULL,
    project_id              UUID,
    active                  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_workers_tenant_document
        UNIQUE (tenant_id, document_type, document_number)
);

CREATE INDEX idx_workers_tenant ON workers(tenant_id);
CREATE INDEX idx_workers_project ON workers(project_id);
