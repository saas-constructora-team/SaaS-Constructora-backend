-- Evidencia fotográfica de etapas de proyecto. Postgres guarda solo metadatos y
-- la clave del objeto; el binario vive en Object Storage (S3-compatible).
-- Toda consulta debe filtrar explícitamente por tenant_id.

CREATE TABLE project_photos (
    id            UUID PRIMARY KEY,
    tenant_id     UUID NOT NULL,
    project_id    UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    stage_id      UUID NOT NULL REFERENCES project_stages(id) ON DELETE CASCADE,
    object_key    VARCHAR(1024) NOT NULL,
    content_type  VARCHAR(150) NOT NULL,
    size_bytes    BIGINT NOT NULL,
    sha256        VARCHAR(64) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    uploaded_by   UUID NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_project_photos_tenant ON project_photos(tenant_id);
CREATE INDEX idx_project_photos_project ON project_photos(project_id);
CREATE INDEX idx_project_photos_stage ON project_photos(stage_id);
CREATE INDEX idx_project_photos_tenant_project_stage
    ON project_photos(tenant_id, project_id, stage_id);
