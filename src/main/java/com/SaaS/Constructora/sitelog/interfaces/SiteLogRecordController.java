package com.SaaS.Constructora.sitelog.interfaces;

import com.SaaS.Constructora.shared.security.AuthenticatedUser;
import com.SaaS.Constructora.shared.security.CurrentUser;
import com.SaaS.Constructora.sitelog.application.SiteLogRecordService;
import com.SaaS.Constructora.sitelog.domain.SiteLogType;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Bitácora de obra: entradas asociadas al proyecto autenticado. */
@RestController
@RequestMapping("/api/projects/{projectId}/site-log")
public class SiteLogRecordController {

    private final SiteLogRecordService service;

    SiteLogRecordController(SiteLogRecordService service) { this.service = service; }

    @PostMapping
    ResponseEntity<SiteLogRecordResponse> create(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID projectId,
            @Valid @RequestBody SiteLogRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(user.tenantId(), projectId, user, request));
    }

    @GetMapping
    List<SiteLogRecordResponse> list(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID projectId,
            @RequestParam(required = false) SiteLogType type,
            @RequestParam(required = false) String q,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return service.list(user.tenantId(), projectId, user, type, q, from, to);
    }

    @PutMapping("/{entryId}")
    SiteLogRecordResponse update(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID projectId,
            @PathVariable UUID entryId,
            @Valid @RequestBody SiteLogRecordRequest request) {
        return service.update(user.tenantId(), projectId, entryId, user, request);
    }

    @PatchMapping("/{entryId}/touch")
    SiteLogRecordResponse touch(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID projectId,
            @PathVariable UUID entryId) {
        return service.touch(user.tenantId(), projectId, entryId, user);
    }

    @DeleteMapping("/{entryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(
            @CurrentUser AuthenticatedUser user,
            @PathVariable UUID projectId,
            @PathVariable UUID entryId) {
        service.delete(user.tenantId(), projectId, entryId, user);
    }
}
