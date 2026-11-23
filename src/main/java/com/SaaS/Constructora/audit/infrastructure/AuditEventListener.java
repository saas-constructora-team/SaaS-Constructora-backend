package com.SaaS.Constructora.audit.infrastructure;

import com.SaaS.Constructora.audit.application.ports.AuditLogRepository;
import com.SaaS.Constructora.audit.domain.AuditLog;
import com.SaaS.Constructora.shared.events.MembershipRevoked;
import com.SaaS.Constructora.shared.events.ProjectAssignmentChanged;
import com.SaaS.Constructora.shared.events.ProjectCreated;
import com.SaaS.Constructora.shared.events.ProjectStageClosed;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Consumidor de auditoría: reacciona AFTER_COMMIT, en su propia transacción. */
@Component
public class AuditEventListener {

    private final AuditLogRepository repository;

    public AuditEventListener(AuditLogRepository repository) {
        this.repository = repository;
    }

    @ApplicationModuleListener
    public void on(ProjectCreated event) {
        repository.save(AuditLog.from(event));
    }

    @ApplicationModuleListener
    public void on(ProjectStageClosed event) {
        repository.save(AuditLog.from(event));
    }

    @ApplicationModuleListener
    public void on(ProjectAssignmentChanged event) {
        repository.save(AuditLog.from(event));
    }

    @ApplicationModuleListener
    public void on(MembershipRevoked event) {
        repository.save(AuditLog.from(event));
    }
}
