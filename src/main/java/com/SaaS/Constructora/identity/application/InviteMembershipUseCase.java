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
import com.SaaS.Constructora.identity.domain.Role;
import com.SaaS.Constructora.identity.domain.Tenant;
import com.SaaS.Constructora.identity.domain.User;
import com.SaaS.Constructora.shared.events.MembershipRevoked;
import com.SaaS.Constructora.shared.security.TokenHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Emite una invitación para que una persona se vincule a una empresa.
 * <p>
 * Caso A: el email ya existe -> no se crea otro {@link User}; solo se crea (o
 * reutiliza) la membresía del tenant en estado INVITED.
 * Caso B: el email no existe -> se crea un {@link User} inactivo con una
 * contraseña aleatoria inutilizable. La persona define su contraseña real al
 * aceptar el enlace. Así el administrador nunca conoce la contraseña.
 */
@Service
public class InviteMembershipUseCase {

    private static final Logger log = LoggerFactory.getLogger(InviteMembershipUseCase.class);
    private static final int MAX_TTL_HOURS = 24 * 30;

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final MembershipInvitationRepository invitationRepository;
    private final TenantRepository tenantRepository;
    private final EmailSender emailSender;
    private final PasswordEncoder passwordEncoder;
    private final TokenHasher tokenHasher;
    private final InvitationGate invitationGate;
    private final ApplicationEventPublisher events;
    private final String baseUrl;
    private final long ttlHours;
    private final SecureRandom random = new SecureRandom();

    public InviteMembershipUseCase(UserRepository userRepository,
                                   MembershipRepository membershipRepository,
                                   MembershipInvitationRepository invitationRepository,
                                   TenantRepository tenantRepository,
                                   EmailSender emailSender,
                                   PasswordEncoder passwordEncoder,
                                   TokenHasher tokenHasher,
                                   InvitationGate invitationGate,
                                    ApplicationEventPublisher events,
                                   @Value("${app.invitation.base-url:http://localhost:3000}") String baseUrl,
                                   @Value("${app.invitation.ttl-hours:72}") long ttlHours) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.invitationRepository = invitationRepository;
        this.tenantRepository = tenantRepository;
        this.emailSender = emailSender;
        this.passwordEncoder = passwordEncoder;
        this.tokenHasher = tokenHasher;
        this.invitationGate = invitationGate;
        this.events = events;
        this.baseUrl = baseUrl;
        this.ttlHours = Math.max(1, Math.min(ttlHours, MAX_TTL_HOURS));
    }

    @Transactional
    public InvitationIssued invite(UUID tenantId, UUID invitedByUserId, String rawEmail,
                                   String fullName, String username, Role requestedRole) {
        invitationGate.requireEnabled();
        String email = normalizeEmail(rawEmail);
        String normalizedUsername = User.normalizeUsername(username);
        if (normalizedUsername != null && !normalizedUsername.matches("^[a-z0-9._-]{3,30}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El username debe tener entre 3 y 30 caracteres y solo puede contener letras minúsculas, números, punto, guion y guion bajo");
        }
        Role role = requestedRole == null ? Role.WORKER : requestedRole;
        if (role == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se puede invitar a un trabajador con rol ADMIN");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Empresa no encontrada"));

        LocalDateTime now = LocalDateTime.now();
        Optional<User> existingUser = userRepository.findByEmail(email);
        User user;
        Membership membership;
        boolean revoked = false;

        if (existingUser.isPresent()) {
            user = existingUser.get();
            if (normalizedUsername != null && !normalizedUsername.equals(user.getUsername())) {
                if (userRepository.existsByUsernameIgnoreCaseAndIdNot(normalizedUsername, user.getId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "El username ya está en uso");
                }
                user = userRepository.save(new User(user.getId(), user.getEmail(), user.getFullName(), normalizedUsername,
                        user.getPasswordHash(), user.isActive(), user.getCreatedAt()));
            }
            Optional<Membership> existingMembership =
                    membershipRepository.findByUserIdAndTenantId(user.getId(), tenantId);
            if (existingMembership.isPresent()) {
                membership = existingMembership.get();
                if (membership.isAccessible()) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "La persona ya es miembro activo de esta empresa");
                }
                membership = membership.withRole(role).reinvited();
                revoked = true;
            } else {
                membership = Membership.invited(
                        UUID.randomUUID(), user.getId(), tenantId, role, false, now);
            }
        } else {
            if (!StringUtils.hasText(fullName)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El nombre completo es obligatorio para invitar a un correo nuevo");
            }
            if (normalizedUsername != null && userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El username ya está en uso");
            }
            user = userRepository.save(new User(
                    UUID.randomUUID(), email, fullName.trim(), normalizedUsername,
                    passwordEncoder.encode(randomUrlSafeToken()), false, now));
            membership = Membership.invited(
                    UUID.randomUUID(), user.getId(), tenantId, role, false, now);
        }

        membershipRepository.save(membership);

        // Re-invitar a alguien que ya tenía acceso lo suspende: projects retira
        // sus asignaciones al consumir este evento (auditoría incluida).
        if (revoked) {
            events.publishEvent(new MembershipRevoked(
                    UUID.randomUUID(), tenantId, membership.getId(), user.getId(),
                    invitedByUserId, now));
        }

        invitationRepository.invalidateActiveForMembership(membership.getId());

        String rawToken = randomUrlSafeToken();
        LocalDateTime expiresAt = now.plusHours(ttlHours);
        invitationRepository.save(new MembershipInvitation(
                UUID.randomUUID(), membership.getId(), tenantId, email, invitedByUserId,
                tokenHasher.hash(rawToken), expiresAt, null, false, now));

        String inviteLink = baseUrl + "/invitations/accept?token=" + rawToken;
        EmailSendResult emailResult = emailSender.sendInvitationEmail(email, user.getFullName(), tenant.getName(),
                inviteLink, ttlHours);
        if (!emailResult.isDelivered()) {
            log.error("[EMAIL] Invitación registrada pero correo NO enviado a {} (membershipId={}): {}",
                    email, membership.getId(), emailResult.error());
        }

        return new InvitationIssued(membership.getId(), user.getId(), email,
                user.getFullName(), user.getUsername(), tenant.getName(), MembershipStatus.INVITED, expiresAt,
                emailResult.isDelivered(), emailResult.error());
    }

    private static String normalizeEmail(String rawEmail) {
        if (!StringUtils.hasText(rawEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El correo electrónico es obligatorio");
        }
        return rawEmail.toLowerCase().trim();
    }

    private String randomUrlSafeToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
