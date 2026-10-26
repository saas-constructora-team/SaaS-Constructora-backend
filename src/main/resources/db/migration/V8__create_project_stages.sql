-- Etapas de un proyecto. El tenant se valida en la capa de aplicación.
CREATE TABLE project_stages (
    id          UUID PRIMARY KEY,
    tenant_id   UUID NOT NULL,
    project_id  UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name        VARCHAR(150) NOT NULL,
    stage_order INTEGER NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_project_stages_tenant_project_order
        UNIQUE (tenant_id, project_id, stage_order)
);

CREATE INDEX idx_project_stages_tenant ON project_stages(tenant_id);
CREATE INDEX idx_project_stages_project ON project_stages(project_id);

-- Las seis etapas iniciales también aplican a los proyectos que ya existían.
INSERT INTO project_stages (id, tenant_id, project_id, name, stage_order)
SELECT gen_random_uuid(), p.tenant_id, p.id, s.name, s.stage_order
FROM projects p
CROSS JOIN (VALUES
    (1, 'Estudios Preliminares y Diagnóstico'),
    (2, 'Anteproyecto'),
    (3, 'Proyecto Ejecutivo'),
    (4, 'Licencias, Permisos y Presupuesto Final'),
    (5, 'Construcción'),
    (6, 'Cierre, Liquidación y Entrega')
) AS s(stage_order, name);
