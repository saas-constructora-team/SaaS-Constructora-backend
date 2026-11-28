-- Las tareas pueden vincularse opcionalmente a una etapa de su proyecto.
-- La referencia se guarda por ID: se valida en la capa de aplicación mediante
-- ProjectService, sin consultar tablas de projects desde este módulo.
ALTER TABLE tasks ADD COLUMN stage_id UUID;
ALTER TABLE tasks ADD CONSTRAINT fk_tasks_stage
    FOREIGN KEY (stage_id) REFERENCES project_stages(id) ON DELETE SET NULL;
CREATE INDEX idx_tasks_stage ON tasks(stage_id);
CREATE INDEX idx_tasks_tenant_project_stage ON tasks(tenant_id, project_id, stage_id);
