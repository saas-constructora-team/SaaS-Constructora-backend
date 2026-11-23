package com.SaaS.Constructora.identity.application;

import com.SaaS.Constructora.identity.application.ports.MembershipRepository;
import com.SaaS.Constructora.identity.application.ports.UserRepository;
import com.SaaS.Constructora.identity.domain.Membership;
import com.SaaS.Constructora.identity.domain.User;
import com.SaaS.Constructora.shared.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Caso de uso HU-01: login inicial de la aplicación.
 * Validación contextual: existencia del usuario, coincidencia de credenciales y estado activo.
 * Toda falla de autenticación responde 401 sin revelar si el email existe.
 * El rol y tenant se obtienen desde Membership (no desde User).
 */
@Service
public class LoginUseCase {

    static final String CREDENCIALES_INVALIDAS = "Credenciales inválidas";
    static final String SIN_MEMBRESIA_ACTIVA = "El usuario no tiene membresía activa en ninguna empresa";

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginUseCase(UserRepository userRepository, MembershipRepository membershipRepository,
                        PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public AuthenticationResult login(LoginCommand command) {
        User user = userRepository.findByEmail(command.email().toLowerCase().trim())
                .filter(u -> passwordEncoder.matches(command.password(), u.getPasswordHash()))
                .filter(User::canAuthenticate)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, CREDENCIALES_INVALIDAS));

        // Obtener la membresía activa del usuario (para obtener tenant y rol)
        Membership membership = membershipRepository.findActiveByUserId(user.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, SIN_MEMBRESIA_ACTIVA));

        String token = jwtService.generateToken(user, membership);
        return new AuthenticationResult(token, jwtService.getTokenTtlSeconds(), user, membership);
    }
}
