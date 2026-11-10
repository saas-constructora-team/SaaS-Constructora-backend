-- Catálogo inicial de cargos por tenant y conservación de los cargos ya usados.

-- Los cargos que antes eran un enum fijo se mantienen disponibles para cada empresa.
INSERT INTO worker_positions (id, tenant_id, name, description, active, created_at, updated_at)
SELECT gen_random_uuid(), t.id, p.name, p.description, TRUE, NOW(), NOW()
FROM tenants t
CROSS JOIN (VALUES
    ('Administración', 'Área administrativa de la empresa'),
    ('Jefe de obra', 'Gestión y dirección de proyectos'),
    ('Encargado de obra', 'Supervisión directa en obra'),
    ('Operario', 'Personal operativo de obra')
) AS p(name, description)
ON CONFLICT (tenant_id, name) DO NOTHING;

-- Normaliza los valores técnicos del enum anterior si existían trabajadores.
UPDATE workers
SET job_position = CASE job_position
    WHEN 'ADMINISTRATIVE' THEN 'Administración'
    WHEN 'PROJECT_MANAGER' THEN 'Jefe de obra'
    WHEN 'SITE_SUPERVISOR' THEN 'Encargado de obra'
    WHEN 'OPERATOR' THEN 'Operario'
    ELSE job_position
END
WHERE job_position IN ('ADMINISTRATIVE', 'PROJECT_MANAGER', 'SITE_SUPERVISOR', 'OPERATOR');

-- Conserva como cargo cualquier otro valor que ya tuvieran los trabajadores.
INSERT INTO worker_positions (id, tenant_id, name, description, active, created_at, updated_at)
SELECT gen_random_uuid(), w.tenant_id, w.job_position, NULL, TRUE, NOW(), NOW()
FROM workers w
GROUP BY w.tenant_id, w.job_position
ON CONFLICT (tenant_id, name) DO NOTHING;

-- El timestamp de actualización debe existir siempre.
UPDATE worker_positions SET updated_at = created_at WHERE updated_at IS NULL;
ALTER TABLE worker_positions ALTER COLUMN updated_at SET NOT NULL;
