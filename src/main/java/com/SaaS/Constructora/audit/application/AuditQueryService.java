package com.SaaS.Constructora.audit.application;

import com.SaaS.Constructora.audit.application.ports.AuditLogRepository;
import com.SaaS.Constructora.audit.domain.AuditLog;
import com.SaaS.Constructora.identity.domain.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuditQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AuditLogRepository repository;

    public AuditQueryService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> search(UUID tenantId, Role role, String eventType,
                                           UUID entityId, UUID actorUserId,
                                           LocalDateTime from, LocalDateTime to,
                                           Pageable pageable) {
        if (!role.isAdmin()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Se requiere rol de administrador para consultar auditoría");
        }
        Sort allowedSort = Sort.by(Sort.Direction.DESC, "occurredAt")
                .and(Sort.by(Sort.Direction.DESC, "id"));
        PageRequest safePage = PageRequest.of(
                Math.max(pageable.getPageNumber(), 0),
                Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE),
                allowedSort);
        return repository.search(tenantId, eventType, entityId, actorUserId, from, to, safePage);
    }
}
