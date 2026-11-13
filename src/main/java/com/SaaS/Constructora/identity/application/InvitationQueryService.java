package com.SaaS.Constructora.identity.application;

import com.SaaS.Constructora.identity.application.ports.MembershipInvitationRepository;
import com.SaaS.Constructora.identity.application.ports.TenantRepository;
import com.SaaS.Constructora.identity.domain.MembershipInvitation;
import com.SaaS.Constructora.identity.domain.Tenant;
import com.SaaS.Constructora.shared.security.TokenHasher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

/**
 * Previsualización pública de una invitación (sin autenticación).
 * Solo revela los datos que ya están en el correo que posee el token.
 */
@Service
public class InvitationQueryService {

    private final MembershipInvitationRepository invitationRepository;
    private final TenantRepository tenantRepository;
    private final TokenHasher tokenHasher;
    private final InvitationGate invitationGate;

    public InvitationQueryService(MembershipInvitationRepository invitationRepository,
                                  TenantRepository tenantRepository,
                                  TokenHasher tokenHasher,
                                  InvitationGate invitationGate) {
        this.invitationRepository = invitationRepository;
        this.tenantRepository = tenantRepository;
        this.tokenHasher = tokenHasher;
        this.invitationGate = invitationGate;
    }

    @Transactional(readOnly = true)
    public InvitationPreview preview(String rawToken) {
        invitationGate.requireEnabled();
        MembershipInvitation invitation = invitationRepository
                .findByTokenHash(tokenHasher.hash(rawToken))
                .filter(i -> i.isUsable(LocalDateTime.now()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "La invitación no existe, expiró o ya fue utilizada"));

        Tenant tenant = tenantRepository.findById(invitation.getTenantId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "La invitación no existe, expiró o ya fue utilizada"));

        return new InvitationPreview(tenant.getName(), invitation.getEmail(),
                invitation.getExpiresAt());
    }
}
