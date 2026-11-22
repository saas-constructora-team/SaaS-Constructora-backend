-- Permiso puntual para que un MEMBER pueda consultar usuarios candidatos
-- a asignación de proyectos. Los ADMIN siempre tienen el permiso por su rol.
ALTER TABLE memberships
    ADD COLUMN can_view_assignment_candidates BOOLEAN NOT NULL DEFAULT FALSE;
