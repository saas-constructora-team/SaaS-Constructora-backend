-- Módulo site-log: asociación 1:1 entre documentos y etapas de proyecto.
-- Las referencias se guardan por ID: site-log no consulta tablas de projects ni
-- de documents (Propiedad de Datos). El binario vive en Neon Object Storage.

CREATE TABLE site_log_entries (
    id          UUID PRIMARY KEY,
    tenant_id   UUID NOT NULL,
    project_id  UUID NOT NULL,
    stage_id    UUID NOT NULL,
    document_id UUID NOT NULL,
    created_by  UUID NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_site_log_document UNIQUE (document_id)
);

CREATE INDEX idx_site_log_tenant ON site_log_entries(tenant_id);
CREATE INDEX idx_site_log_project ON site_log_entries(project_id);
CREATE INDEX idx_site_log_stage ON site_log_entries(stage_id);
CREATE INDEX idx_site_log_tenant_project_stage
    ON site_log_entries(tenant_id, project_id, stage_id);
