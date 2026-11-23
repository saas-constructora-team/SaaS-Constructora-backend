-- Búsqueda de proyectos: orden por fecha, filtros exactos y texto parcial.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_projects_tenant_created
    ON projects (tenant_id, created_at DESC, id DESC);
CREATE INDEX idx_projects_tenant_status_created
    ON projects (tenant_id, status, created_at DESC, id DESC);
CREATE INDEX idx_projects_tenant_type_created
    ON projects (tenant_id, project_type, created_at DESC, id DESC);

-- Subconsulta EXISTS de visibilidad para usuarios no administradores.
CREATE INDEX idx_project_assignments_tenant_user_project
    ON project_assignments (tenant_id, user_id, project_id);

-- lower(name) LIKE :pattern con comodines usa estos índices GIN de trigramas.
CREATE INDEX idx_projects_name_trgm
    ON projects USING gin (lower(name) gin_trgm_ops);
CREATE INDEX idx_projects_client_name_trgm
    ON projects USING gin (lower(client_name) gin_trgm_ops);
