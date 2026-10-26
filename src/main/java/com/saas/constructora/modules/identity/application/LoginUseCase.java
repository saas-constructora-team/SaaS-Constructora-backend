package com.saas.constructora.modules.identity.application;

import com.saas.constructora.modules.identity.application.ports.UserRepository;
import com.saas.constructora.modules.identity.domain.User;
import com.saas.constructora.modules.identity.interfaces.dto.LoginRequest;
import com.saas.constructora.modules.identity.interfaces.dto.LoginResponse;
import com.saas.constructora.shared.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Caso de uso HU-01: login inicial de la aplicación.
 * Validación contextual: existencia del usuario, coincidencia de credenciales y estado activo.
 * Toda falla de autenticación responde 401 sin revelar si el email existe.
 */
@Service
public class LoginUseCase {

    static final String CREDENCIALES_INVALIDAS = "Credenciales inválidas";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginUseCase(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase().trim())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .filter(User::canAuthenticate)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, CREDENCIALES_INVALIDAS));

        String token = jwtService.generateToken(user);
        return LoginResponse.from(token, jwtService.getTokenTtlSeconds(), user);
    }
}
