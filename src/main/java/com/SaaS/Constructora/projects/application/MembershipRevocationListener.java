package com.SaaS.Constructora.projects.application;

import com.SaaS.Constructora.shared.events.MembershipRevoked;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Aplica AFTER_COMMIT la revocación de membresía sobre las asignaciones. */
@Component
public class MembershipRevocationListener {

    private final ProjectAssignmentService assignmentService;

    public MembershipRevocationListener(ProjectAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @ApplicationModuleListener
    public void on(MembershipRevoked event) {
        assignmentService.unassignAllForUser(
                event.tenantId(), event.userId(), event.actorUserId());
    }
}
