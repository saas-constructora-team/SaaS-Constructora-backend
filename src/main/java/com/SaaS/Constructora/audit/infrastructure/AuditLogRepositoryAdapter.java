package com.SaaS.Constructora.audit.infrastructure;

import com.SaaS.Constructora.audit.application.ports.AuditLogRepository;
import com.SaaS.Constructora.audit.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
class AuditLogRepositoryAdapter implements AuditLogRepository {

    private final AuditLogJpaRepository repository;

    AuditLogRepositoryAdapter(AuditLogJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void save(AuditLog entry) {
        // Modulith puede reentregar un evento; el eventId evita duplicar la entrada.
        if (!repository.existsById(entry.getId())) {
            repository.save(entry);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> search(UUID tenantId, String eventType, UUID entityId,
                                 UUID actorUserId, LocalDateTime from, LocalDateTime to,
                                 Pageable pageable) {
        return repository.search(tenantId, eventType, entityId, actorUserId,
                from, to, pageable);
    }
}
