-- Fecha prevista, campos específicos por área y estado 'Terminado' protegido.
-- El tenant sigue viajando en el contexto autenticado: todas las tablas nuevas
-- guardan su tenant_id o viven en la fila de tasks del tenant correspondiente.

-- Estados del sistema. Se renombra 'Hecho' solo si el proyecto no tiene ya un
-- 'Terminado' propio, para no chocar con el constraint de nombre único.
ALTER TABLE task_statuses ADD COLUMN is_system BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE task_statuses ts
SET name = 'Terminado', is_system = TRUE
WHERE ts.name = 'Hecho'
  AND NOT EXISTS (
      SELECT 1 FROM task_statuses otra
      WHERE otra.tenant_id = ts.tenant_id
        AND otra.project_id = ts.project_id
        AND otra.name = 'Terminado'
  );

UPDATE task_statuses SET is_system = TRUE WHERE name IN ('Hecho', 'Terminado');

-- Áreas predeterminadas. El tipo es inmutable: permite renombrar el área sin
-- perder la asociación con los campos de la tarea.
ALTER TABLE project_areas ADD COLUMN tipo VARCHAR(40) NOT NULL DEFAULT 'PERSONALIZADA';

UPDATE project_areas
SET tipo = CASE name
    WHEN 'Obra / Ingeniería' THEN 'OBRA_INGENIERIA'
    WHEN 'Compras y Almacén' THEN 'COMPRAS_ALMACEN'
    WHEN 'Diseño y Arquitectura' THEN 'DISENO_ARQUITECTURA'
    WHEN 'Legal / Trámites' THEN 'LEGAL_TRAMITES'
    WHEN 'Contabilidad / Finanzas' THEN 'CONTABILIDAD_FINANZAS'
    ELSE 'PERSONALIZADA'
END
WHERE name IN ('Obra / Ingeniería', 'Compras y Almacén', 'Diseño y Arquitectura',
               'Legal / Trámites', 'Contabilidad / Finanzas');

INSERT INTO project_areas (id, tenant_id, project_id, name, tipo, created_at)
SELECT gen_random_uuid(), p.tenant_id, p.id, a.name, a.tipo, NOW()
FROM projects p
CROSS JOIN (VALUES
    ('Obra / Ingeniería', 'OBRA_INGENIERIA'),
    ('Compras y Almacén', 'COMPRAS_ALMACEN'),
    ('Diseño y Arquitectura', 'DISENO_ARQUITECTURA'),
    ('Legal / Trámites', 'LEGAL_TRAMITES'),
    ('Contabilidad / Finanzas', 'CONTABILIDAD_FINANZAS')
) AS a(name, tipo)
WHERE NOT EXISTS (
    SELECT 1 FROM project_areas pa
    WHERE pa.tenant_id = p.tenant_id AND pa.project_id = p.id AND pa.name = a.name
);

-- Detalles de la tarea según el área elegida. 'contratista_id' y
-- 'proveedor_sugerido' no son FK: no existe entidad de proveedores en el
-- backend y está prohibido acoplar tablas de otros módulos.
ALTER TABLE tasks
    ADD COLUMN fecha_fin_prevista DATE,
    ADD COLUMN frente_obra VARCHAR(300),
    ADD COLUMN capitulo_presupuestal VARCHAR(40),
    ADD COLUMN unidad_medida VARCHAR(20),
    ADD COLUMN cantidad_programada NUMERIC(18, 3),
    ADD COLUMN contratista_id UUID,
    ADD COLUMN requiere_permiso_sst BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN permisos_sst VARCHAR(40)[],
    ADD COLUMN codigo_requisicion VARCHAR(80),
    ADD COLUMN fecha_requerida_en_obra DATE,
    ADD COLUMN categoria_insumo VARCHAR(40),
    ADD COLUMN presupuesto_estimado NUMERIC(18, 2),
    ADD COLUMN proveedor_sugerido VARCHAR(150),
    ADD COLUMN disciplina VARCHAR(40),
    ADD COLUMN codigo_plano VARCHAR(80),
    ADD COLUMN version_plano VARCHAR(20),
    ADD COLUMN entidad VARCHAR(150),
    ADD COLUMN numero_radicado VARCHAR(80),
    ADD COLUMN tipo_documento VARCHAR(40),
    ADD COLUMN numero_documento VARCHAR(80),
    ADD COLUMN monto NUMERIC(18, 2);
