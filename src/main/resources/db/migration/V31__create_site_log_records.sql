-- Módulo site-log: entradas de la bitácora de obra (una por proyecto).
-- Las referencias se guardan por ID: no hay FK cruzadas con projects ni identity.
CREATE TABLE site_log_records (
    id           UUID PRIMARY KEY,
    tenant_id    UUID NOT NULL,
    project_id   UUID NOT NULL,
    type         VARCHAR(20) NOT NULL,
    status       VARCHAR(20) NOT NULL,
    what         VARCHAR(500) NOT NULL,
    how          TEXT NOT NULL,
    event_at     TIMESTAMP NOT NULL,
    location     VARCHAR(300) NOT NULL,
    why          TEXT,
    author_id    UUID NOT NULL,
    assignee_id  UUID,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sitelog_records_tenant_project
    ON site_log_records(tenant_id, project_id);
CREATE INDEX idx_sitelog_records_project_event
    ON site_log_records(project_id, event_at DESC);
