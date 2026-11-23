-- Tabla de propiedad del módulo audit. El registro transaccional de Spring
-- Modulith (event_publication) garantiza la entrega; esta tabla es el historial
-- consultable por tenant, acción, entidad y actor.
CREATE TABLE audit_log (
    event_id       UUID PRIMARY KEY,
    tenant_id      UUID NOT NULL,
    event_type     VARCHAR(100) NOT NULL,
    entity_type    VARCHAR(100) NOT NULL,
    entity_id      UUID NOT NULL,
    actor_user_id  UUID,
    occurred_at    TIMESTAMP NOT NULL,
    summary        VARCHAR(1000)
);

CREATE INDEX idx_audit_log_tenant_occurred
    ON audit_log (tenant_id, occurred_at DESC, event_id DESC);
CREATE INDEX idx_audit_log_tenant_type_occurred
    ON audit_log (tenant_id, event_type, occurred_at DESC, event_id DESC);
CREATE INDEX idx_audit_log_tenant_entity_occurred
    ON audit_log (tenant_id, entity_type, entity_id, occurred_at DESC);
CREATE INDEX idx_audit_log_tenant_actor_occurred
    ON audit_log (tenant_id, actor_user_id, occurred_at DESC);
