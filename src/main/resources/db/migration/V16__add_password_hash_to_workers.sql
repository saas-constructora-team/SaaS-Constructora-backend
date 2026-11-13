-- Agregar columna para almacenar el hash de la contraseña del trabajador.
-- La columna es nullable para no bloquear migraciones existentes; las nuevas
-- filas se poblaran desde PersonnelService.create().
ALTER TABLE workers
    ADD COLUMN password_hash VARCHAR(100);

-- Agrega columna user_id para vincular el worker a su registro de autenticación.
-- Nullable al inicio; los workers creados vía PersonnelService tendrán valor.
ALTER TABLE workers
    ADD COLUMN user_id UUID;