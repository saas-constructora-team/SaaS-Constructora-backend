-- Tareas de los proyectos. El tenant viaja en el contexto autenticado y el
-- acceso al proyecto se valida en la capa de aplicación mediante ProjectService.
CREATE TABLE tasks (
    id          UUID PRIMARY KEY,
    tenant_id   UUID NOT NULL,
    project_id  UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    title       VARCHAR(150) NOT NULL,
    description VARCHAR(2000),
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_by  UUID NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tasks_tenant ON tasks(tenant_id);
CREATE INDEX idx_tasks_project ON tasks(project_id);
CREATE INDEX idx_tasks_tenant_project ON tasks(tenant_id, project_id);
