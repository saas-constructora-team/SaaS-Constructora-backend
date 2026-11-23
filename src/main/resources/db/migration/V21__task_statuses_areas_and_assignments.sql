-- Catálogo configurable de estados por proyecto y áreas por proyecto.
-- El tenant viaja en el contexto autenticado; toda consulta debe filtrarlo.
CREATE TABLE task_statuses (
    id            UUID PRIMARY KEY,
    tenant_id     UUID NOT NULL,
    project_id    UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name          VARCHAR(80) NOT NULL,
    color         VARCHAR(7) NOT NULL,
    status_order  INTEGER NOT NULL,
    is_default    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_task_statuses_tenant_project_name
        UNIQUE (tenant_id, project_id, name),
    CONSTRAINT uk_task_statuses_tenant_project_order
        UNIQUE (tenant_id, project_id, status_order)
);

CREATE UNIQUE INDEX uk_task_statuses_default
    ON task_statuses(tenant_id, project_id) WHERE is_default;

CREATE TABLE project_areas (
    id          UUID PRIMARY KEY,
    tenant_id   UUID NOT NULL,
    project_id  UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    name        VARCHAR(150) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_project_areas_tenant_project_name
        UNIQUE (tenant_id, project_id, name)
);

CREATE INDEX idx_task_statuses_tenant ON task_statuses(tenant_id);
CREATE INDEX idx_task_statuses_project ON task_statuses(project_id);
CREATE INDEX idx_project_areas_tenant ON project_areas(tenant_id);
CREATE INDEX idx_project_areas_project ON project_areas(project_id);

-- Estados iniciales, también para los proyectos ya existentes.
INSERT INTO task_statuses (id, tenant_id, project_id, name, color, status_order, is_default)
SELECT gen_random_uuid(), p.tenant_id, p.id, s.name, s.color, s.status_order, s.is_default
FROM projects p
CROSS JOIN (VALUES
    (1, 'Pendiente', '#EF4444', TRUE),
    (2, 'En curso', '#F59E0B', FALSE),
    (3, 'Hecho', '#22C55E', FALSE)
) AS s(status_order, name, color, is_default);

-- Las tareas existentes conservan su estado mapeado al catálogo nuevo.
ALTER TABLE tasks ADD COLUMN status_id UUID;

UPDATE tasks t
SET status_id = ts.id
FROM task_statuses ts
WHERE ts.tenant_id = t.tenant_id
  AND ts.project_id = t.project_id
  AND ts.name = CASE t.status
      WHEN 'PENDING' THEN 'Pendiente'
      WHEN 'IN_PROGRESS' THEN 'En curso'
      WHEN 'DONE' THEN 'Hecho'
      ELSE 'Pendiente'
  END;

ALTER TABLE tasks ALTER COLUMN status_id SET NOT NULL;
ALTER TABLE tasks ADD CONSTRAINT fk_tasks_status
    FOREIGN KEY (status_id) REFERENCES task_statuses(id);

ALTER TABLE tasks ADD COLUMN area_id UUID;
ALTER TABLE tasks ADD CONSTRAINT fk_tasks_area
    FOREIGN KEY (area_id) REFERENCES project_areas(id) ON DELETE SET NULL;

-- Se guarda la asignación del proyecto, no solo el usuario: así el vínculo
-- con el proyecto se mantiene aunque cambie el rol dentro de la asignación.
ALTER TABLE tasks ADD COLUMN assignee_assignment_id UUID;
ALTER TABLE tasks ADD CONSTRAINT fk_tasks_assignee
    FOREIGN KEY (assignee_assignment_id) REFERENCES project_assignments(id) ON DELETE SET NULL;

ALTER TABLE tasks ADD COLUMN validator_assignment_id UUID;
ALTER TABLE tasks ADD CONSTRAINT fk_tasks_validator
    FOREIGN KEY (validator_assignment_id) REFERENCES project_assignments(id) ON DELETE SET NULL;

ALTER TABLE tasks DROP COLUMN status;

CREATE INDEX idx_tasks_status ON tasks(status_id);
CREATE INDEX idx_tasks_area ON tasks(area_id);
CREATE INDEX idx_tasks_assignee ON tasks(assignee_assignment_id);
CREATE INDEX idx_tasks_validator ON tasks(validator_assignment_id);
