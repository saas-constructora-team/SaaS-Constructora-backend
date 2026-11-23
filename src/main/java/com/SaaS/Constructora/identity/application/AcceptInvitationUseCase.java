package com.SaaS.Constructora.identity.application;

import com.SaaS.Constructora.identity.application.ports.MembershipInvitationRepository;
import com.SaaS.Constructora.identity.application.ports.MembershipRepository;
import com.SaaS.Constructora.identity.application.ports.UserRepository;
import com.SaaS.Constructora.identity.domain.Membership;
import com.SaaS.Constructora.identity.domain.MembershipInvitation;
import com.SaaS.Constructora.identity.domain.User;
import com.SaaS.Constructora.shared.security.JwtService;
import com.SaaS.Constructora.shared.security.TokenHasher;
import com.SaaS.Constructora.shared.validation.PasswordPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

/**
 * Consume el token de una invitación.
 * <p>
 * Si la cuenta es nueva (creada inactiva al invitar), exige definir contraseña;
 * si la persona ya tenía una cuenta activa, solo activa la nueva membresía.
 * En ambos casos devuelve una sesión JWT del tenant invitado.
 */
@Service
public class AcceptInvitationUseCase {

    private static final String INVITACION_INVALIDA =
            "La invitación no existe, expiró o ya fue utilizada";

    private final MembershipInvitationRepository invitationRepository;
    private final MembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenHasher tokenHasher;
    private final JwtService jwtService;
    private final InvitationGate invitationGate;

    public AcceptInvitationUseCase(MembershipInvitationRepository invitationRepository,
                                   MembershipRepository membershipRepository,
                                   UserRepository userRepository,
                                   PasswordEncoder passwordEncoder,
                                   TokenHasher tokenHasher,
                                   JwtService jwtService,
                                   InvitationGate invitationGate) {
        this.invitationRepository = invitationRepository;
        this.membershipRepository = membershipRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenHasher = tokenHasher;
        this.jwtService = jwtService;
        this.invitationGate = invitationGate;
    }

    @Transactional
    public AuthenticationResult accept(AcceptInvitationCommand request) {
        invitationGate.requireEnabled();
        LocalDateTime now = LocalDateTime.now();
        MembershipInvitation invitation = invitationRepository
                .findByTokenHash(tokenHasher.hash(request.token()))
                .filter(i -> i.isUsable(now))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, INVITACION_INVALIDA));

        Membership membership = membershipRepository
                .findByIdAndTenantId(invitation.getMembershipId(), invitation.getTenantId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, INVITACION_INVALIDA));

        User user = userRepository.findById(membership.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, INVITACION_INVALIDA));

        if (!user.isActive()) {
            if (!StringUtils.hasText(request.password())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Debe definir una contraseña para activar su cuenta");
            }
            if (!PasswordPolicy.isValid(request.password())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, PasswordPolicy.MESSAGE);
            }
            userRepository.updatePasswordHashAndActivate(
                    user.getId(), passwordEncoder.encode(request.password()));
        }

        if (!membership.isAccessible()) {
            membership = membership.activate();
            membershipRepository.save(membership);
        }

        invitationRepository.save(invitation.acceptedAt(now));
        invitationRepository.invalidateActiveForMembership(membership.getId());

        String token = jwtService.generateToken(user, membership);
        return new AuthenticationResult(token, jwtService.getTokenTtlSeconds(), user, membership);
    }
}
