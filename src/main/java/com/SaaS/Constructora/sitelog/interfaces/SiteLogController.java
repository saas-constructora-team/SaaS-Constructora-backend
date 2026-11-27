package com.SaaS.Constructora.sitelog.interfaces;

import com.SaaS.Constructora.shared.security.AuthenticatedUser;
import com.SaaS.Constructora.shared.security.CurrentUser;
import com.SaaS.Constructora.sitelog.application.SiteLogService;
import com.SaaS.Constructora.sitelog.domain.SiteLogEntry;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Asociación 1:1 de documentos a etapas; el binario sigue en Neon Object Storage. */
@RestController
@RequestMapping("/api/projects/{projectId}/stages/{stageId}/documents")
public class SiteLogController {

    private final SiteLogService service;

    SiteLogController(SiteLogService service) { this.service = service; }

    @PostMapping
    ResponseEntity<SiteLogEntryResponse> associate(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID projectId,
            @PathVariable UUID stageId,
            @Valid @RequestBody AssociateDocumentRequest request) {
        SiteLogEntry entry = service.associate(user.tenantId(), projectId, stageId,
                user.userId(), user.role(), request.documentId());
        return ResponseEntity.status(HttpStatus.CREATED).body(SiteLogEntryResponse.from(entry));
    }

    @GetMapping
    List<SiteLogEntryResponse> list(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID projectId,
            @PathVariable UUID stageId) {
        return service.list(user.tenantId(), projectId, stageId, user.userId(), user.role())
                .stream()
                .map(SiteLogEntryResponse::from)
                .toList();
    }
}
