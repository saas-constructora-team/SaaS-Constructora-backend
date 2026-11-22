package com.SaaS.Constructora.identity.application;

import com.SaaS.Constructora.identity.application.ports.EmailSendResult;
import com.SaaS.Constructora.identity.application.ports.EmailSender;
import com.SaaS.Constructora.identity.application.ports.MembershipInvitationRepository;
import com.SaaS.Constructora.identity.application.ports.MembershipRepository;
import com.SaaS.Constructora.identity.application.ports.TenantRepository;
import com.SaaS.Constructora.identity.application.ports.UserRepository;
import com.SaaS.Constructora.identity.domain.Membership;
import com.SaaS.Constructora.identity.domain.MembershipInvitation;
import com.SaaS.Constructora.identity.domain.MembershipStatus;
import com.SaaS.Constructora.identity.domain.Tenant;
import com.SaaS.Constructora.identity.domain.User;
import com.SaaS.Constructora.shared.security.TokenHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

/**
 * Reenvía la invitación de una membresía todavía pendiente. Invalida el token
 * anterior para que el enlace previo deje de funcionar.
 */
@Service
public class ResendInvitationUseCase {

    private static final Logger log = LoggerFactory.getLogger(ResendInvitationUseCase.class);
    private static final int MAX_TTL_HOURS = 24 * 30;

    private final PersonnelPermissionService permissionService;
    private final MembershipRepository membershipRepository;
    private final MembershipInvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final EmailSender emailSender;
    private final TokenHasher tokenHasher;
    private final InvitationGate invitationGate;
    private final String baseUrl;
    private final long ttlHours;
    private final SecureRandom random = new SecureRandom();

    public ResendInvitationUseCase(PersonnelPermissionService permissionService,
                                   MembershipRepository membershipRepository,
                                   MembershipInvitationRepository invitationRepository,
                                   UserRepository userRepository,
                                   TenantRepository tenantRepository,
                                   EmailSender emailSender,
                                   TokenHasher tokenHasher,
                                   InvitationGate invitationGate,
                                   @Value("${app.invitation.base-url:http://localhost:3000}") String baseUrl,
                                   @Value("${app.invitation.ttl-hours:72}") long ttlHours) {
        this.permissionService = permissionService;
        this.membershipRepository = membershipRepository;
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.emailSender = emailSender;
        this.tokenHasher = tokenHasher;
        this.invitationGate = invitationGate;
        this.baseUrl = baseUrl;
        this.ttlHours = Math.max(1, Math.min(ttlHours, MAX_TTL_HOURS));
    }

    @Transactional
    public InvitationIssued resend(UUID tenantId, UUID actorId, UUID membershipId) {
        invitationGate.requireEnabled();
        permissionService.requireCanManagePersonnel(tenantId, actorId);

        Membership membership = membershipRepository.findByIdAndTenantId(membershipId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Invitación no encontrada"));

        if (!membership.isInvited()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La membresía ya no está pendiente de aceptación");
        }

        User user = userRepository.findById(membership.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Empresa no encontrada"));

        LocalDateTime now = LocalDateTime.now();
        invitationRepository.invalidateActiveForMembership(membershipId);

        String rawToken = randomUrlSafeToken();
        LocalDateTime expiresAt = now.plusHours(ttlHours);
        invitationRepository.save(new MembershipInvitation(
                UUID.randomUUID(), membershipId, tenantId, user.getEmail(), actorId,
                tokenHasher.hash(rawToken), expiresAt, null, false, now));

        String inviteLink = baseUrl + "/invitations/accept?token=" + rawToken;
        EmailSendResult emailResult = emailSender.sendInvitationEmail(user.getEmail(), user.getFullName(), tenant.getName(),
                inviteLink, ttlHours);
        if (!emailResult.isDelivered()) {
            log.error("[EMAIL] Reenvío registrado pero correo NO enviado a {} (membershipId={}): {}",
                    user.getEmail(), membershipId, emailResult.error());
        }

        return new InvitationIssued(membershipId, user.getId(), user.getEmail(),
                user.getFullName(), tenant.getName(), MembershipStatus.INVITED, expiresAt,
                emailResult.isDelivered(), emailResult.error());
    }

    private String randomUrlSafeToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
