-- La etapa en curso deja de vivir en el navegador: es estado compartido del
-- proyecto, así que se persiste para que todos los miembros con acceso vean
-- el mismo avance. Al borrar la etapa seleccionada queda sin etapa en curso.
ALTER TABLE projects
    ADD COLUMN current_stage_id UUID REFERENCES project_stages(id) ON DELETE SET NULL,
    ADD COLUMN stage_updated_at TIMESTAMP,
    ADD COLUMN stage_updated_by UUID,
    ADD COLUMN version INTEGER NOT NULL DEFAULT 0;

CREATE INDEX idx_projects_current_stage ON projects(current_stage_id);
