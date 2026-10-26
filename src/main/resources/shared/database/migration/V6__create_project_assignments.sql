-- Módulo projects: asignación de personal a proyectos.
-- No hay FK a tenants: el tenant se valida en la capa de aplicación.
CREATE TABLE project_assignments (
    id          UUID PRIMARY KEY,
    tenant_id   UUID NOT NULL,
    project_id  UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    user_id     UUID NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_project_assignments_tenant_project_user
        UNIQUE (tenant_id, project_id, user_id)
);

CREATE INDEX idx_project_assignments_tenant ON project_assignments(tenant_id);
CREATE INDEX idx_project_assignments_project ON project_assignments(project_id);
