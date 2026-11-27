-- Sobrescritura de la versión vigente y retención de los últimos 3 binarios.
-- Los metadatos de todas las versiones se conservan; solo se podan objetos S3.

ALTER TABLE document_versions ADD COLUMN changed_at TIMESTAMP;
UPDATE document_versions SET changed_at = created_at;
ALTER TABLE document_versions ALTER COLUMN changed_at SET NOT NULL;

CREATE TABLE document_version_changes (
    id            UUID PRIMARY KEY,
    tenant_id     UUID NOT NULL,
    document_id   UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    version_id    UUID NOT NULL REFERENCES document_versions(id) ON DELETE CASCADE,
    object_key    VARCHAR(1024) NOT NULL,
    changed_at    TIMESTAMP NOT NULL,
    pruned_at     TIMESTAMP,
    CONSTRAINT uq_document_version_change_object_key UNIQUE (object_key)
);

CREATE INDEX idx_document_version_changes_tenant_document
    ON document_version_changes(tenant_id, document_id, changed_at DESC);
CREATE INDEX idx_document_version_changes_version
    ON document_version_changes(version_id);

-- El historial previo ya tiene un único objeto por versión.
INSERT INTO document_version_changes
    (id, tenant_id, document_id, version_id, object_key, changed_at, pruned_at)
SELECT gen_random_uuid(), tenant_id, document_id, id, object_key, created_at, NULL
FROM document_versions;
