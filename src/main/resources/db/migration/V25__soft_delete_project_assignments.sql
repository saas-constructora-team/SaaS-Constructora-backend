-- Borrado lógico de asignaciones: conserva quién y cuándo retiró a cada usuario.
ALTER TABLE project_assignments
    ADD COLUMN assigned_by UUID,
    ADD COLUMN unassigned_at TIMESTAMP,
    ADD COLUMN unassigned_by UUID,
    ADD COLUMN version INTEGER NOT NULL DEFAULT 0;

-- La unicidad solo aplica a las asignaciones vigentes, para poder reasignar
-- a la misma persona después de retirarla.
ALTER TABLE project_assignments
    DROP CONSTRAINT uk_project_assignments_tenant_project_user;

CREATE UNIQUE INDEX uk_project_assignments_active
    ON project_assignments (tenant_id, project_id, user_id)
    WHERE unassigned_at IS NULL;

CREATE INDEX idx_project_assignments_active_tenant_user
    ON project_assignments (tenant_id, user_id)
    WHERE unassigned_at IS NULL;
