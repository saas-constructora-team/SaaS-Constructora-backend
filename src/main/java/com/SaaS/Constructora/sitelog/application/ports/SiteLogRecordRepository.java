package com.SaaS.Constructora.sitelog.application.ports;

import com.SaaS.Constructora.sitelog.domain.SiteLogRecord;
import com.SaaS.Constructora.sitelog.domain.SiteLogType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistencia de entradas de bitácora, siempre filtrada por tenant. */
public interface SiteLogRecordRepository {

    SiteLogRecord save(SiteLogRecord record);

    Optional<SiteLogRecord> findById(UUID id, UUID tenantId);

    List<SiteLogRecord> findByTenantIdAndProjectId(
            UUID tenantId, UUID projectId, SiteLogType type, String query,
            LocalDateTime from, LocalDateTime to);

    void deleteById(UUID id, UUID tenantId);
}
