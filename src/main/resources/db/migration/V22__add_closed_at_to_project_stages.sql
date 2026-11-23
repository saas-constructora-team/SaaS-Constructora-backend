-- Estado explícito de cierre de etapa. NULL = etapa abierta.
ALTER TABLE project_stages ADD COLUMN closed_at TIMESTAMP;
