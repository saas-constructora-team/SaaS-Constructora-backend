-- Módulo projects: rol del usuario dentro del proyecto.
-- MEMBER: miembro regular (comportamiento anterior).
-- LEAD:   delegación explícita de un ADMIN para gestionar los miembros del proyecto.
ALTER TABLE project_assignments
    ADD COLUMN role VARCHAR(30) NOT NULL DEFAULT 'MEMBER';

CREATE INDEX idx_project_assignments_project_role
    ON project_assignments(project_id, role);
