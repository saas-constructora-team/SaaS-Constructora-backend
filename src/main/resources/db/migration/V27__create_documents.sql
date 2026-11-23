-- Módulo documents (HU-04). Postgres guarda solo metadatos y la clave del
-- objeto; el binario vive en Neon Object Storage (S3-compatible).
-- El tenant viaja en el contexto autenticado y toda consulta debe filtrarlo.

CREATE TABLE documents (
    id                 UUID PRIMARY KEY,
    tenant_id          UUID NOT NULL,
    project_id         UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    kind               VARCHAR(20) NOT NULL,
    title              VARCHAR(200) NOT NULL,
    current_version_id UUID,
    created_by         UUID NOT NULL,
    created_at         TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_documents_tenant ON documents(tenant_id);
CREATE INDEX idx_documents_project ON documents(project_id);
CREATE INDEX idx_documents_tenant_project ON documents(tenant_id, project_id);

CREATE TABLE document_versions (
    id            UUID PRIMARY KEY,
    tenant_id     UUID NOT NULL,
    document_id   UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    version_label VARCHAR(20) NOT NULL,
    object_key    VARCHAR(1024) NOT NULL,
    content_type  VARCHAR(150) NOT NULL,
    size_bytes    BIGINT NOT NULL,
    sha256        VARCHAR(64) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    uploaded_by   UUID NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_document_version_label UNIQUE (document_id, version_label)
);

CREATE INDEX idx_document_versions_tenant ON document_versions(tenant_id);
CREATE INDEX idx_document_versions_document ON document_versions(document_id);
CREATE INDEX idx_document_versions_tenant_document
    ON document_versions(tenant_id, document_id);
