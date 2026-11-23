-- Permiso puntual para que un WORKER o MEMBER pueda crear tareas en los
-- proyectos a los que tiene acceso. Los ADMIN siempre tienen el permiso por su rol.
ALTER TABLE memberships
    ADD COLUMN can_create_tasks BOOLEAN NOT NULL DEFAULT FALSE;
