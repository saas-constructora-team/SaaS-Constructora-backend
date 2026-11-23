package com.SaaS.Constructora.audit.interfaces;

import com.SaaS.Constructora.audit.application.AuditQueryService;
import com.SaaS.Constructora.audit.interfaces.dto.AuditEventResponse;
import com.SaaS.Constructora.shared.security.AuthenticatedUser;
import com.SaaS.Constructora.shared.security.CurrentUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

/** Historial de auditoría del tenant. Solo ADMIN, según AuditQueryService. */
@RestController
@RequestMapping("/api/audit/events")
class AuditController {

    private final AuditQueryService service;

    AuditController(AuditQueryService service) {
        this.service = service;
    }

    @GetMapping
    Page<AuditEventResponse> search(
            @CurrentUser AuthenticatedUser user,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) UUID actorUserId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.search(user.tenantId(), user.role(), eventType, entityId, actorUserId,
                        from, to, PageRequest.of(Math.max(page, 0), Math.max(size, 1)))
                .map(AuditEventResponse::from);
    }
}
