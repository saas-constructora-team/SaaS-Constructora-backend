package com.SaaS.Constructora.audit.application.ports;

import com.SaaS.Constructora.audit.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

/** Contrato del módulo audit: ningún otro módulo accede a su persistencia. */
public interface AuditLogRepository {

    void save(AuditLog entry);

    Page<AuditLog> search(UUID tenantId, String eventType, UUID entityId, UUID actorUserId,
                          LocalDateTime from, LocalDateTime to, Pageable pageable);
}
