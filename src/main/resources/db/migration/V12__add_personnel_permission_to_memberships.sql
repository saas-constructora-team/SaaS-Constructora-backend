-- Permiso puntual para que un MEMBER pueda crear y gestionar el personal
-- de su empresa. Los ADMIN siempre tienen el permiso por su rol.
ALTER TABLE memberships
    ADD COLUMN can_create_personnel BOOLEAN NOT NULL DEFAULT FALSE;
