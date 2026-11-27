-- Permiso puntual para que un WORKER o MEMBER edite/elimine entradas de la
-- bitácora. Los ADMIN siempre tienen el permiso por su rol.
ALTER TABLE memberships
    ADD COLUMN can_manage_site_log BOOLEAN NOT NULL DEFAULT FALSE;
