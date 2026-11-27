package com.SaaS.Constructora.sitelog.infrastructure;

import com.SaaS.Constructora.sitelog.application.ports.SiteLogRecordRepository;
import com.SaaS.Constructora.sitelog.domain.SiteLogRecord;
import com.SaaS.Constructora.sitelog.domain.SiteLogType;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/** Adaptador JPA de las entradas de bitácora. */
@Repository
public class SiteLogRecordRepositoryAdapter implements SiteLogRecordRepository {

    private final SiteLogRecordJpaRepository repository;

    public SiteLogRecordRepositoryAdapter(SiteLogRecordJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public SiteLogRecord save(SiteLogRecord record) {
        return repository.saveAndFlush(new SiteLogRecordEntity(record)).toDomain();
    }

    @Override
    public Optional<SiteLogRecord> findById(UUID id, UUID tenantId) {
        return repository.findByIdAndTenantId(id, tenantId).map(SiteLogRecordEntity::toDomain);
    }

    @Override
    public List<SiteLogRecord> findByTenantIdAndProjectId(
            UUID tenantId, UUID projectId, SiteLogType type, String query,
            LocalDateTime from, LocalDateTime to) {
        String term = query == null ? null : query.trim().toLowerCase(Locale.ROOT);
        return repository.findAllByTenantIdAndProjectIdOrderByEventAtDesc(tenantId, projectId)
                .stream()
                .map(SiteLogRecordEntity::toDomain)
                .filter(record -> type == null || record.type() == type)
                .filter(record -> from == null || !record.eventAt().isBefore(from))
                .filter(record -> to == null || !record.eventAt().isAfter(to))
                .filter(record -> term == null || term.isBlank() || matches(record, term))
                .toList();
    }

    @Override
    public void deleteById(UUID id, UUID tenantId) {
        repository.deleteByIdAndTenantId(id, tenantId);
    }

    private boolean matches(SiteLogRecord record, String term) {
        return contains(record.what(), term)
                || contains(record.how(), term)
                || contains(record.location(), term)
                || contains(record.why(), term);
    }

    private boolean contains(String value, String term) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(term);
    }
}
