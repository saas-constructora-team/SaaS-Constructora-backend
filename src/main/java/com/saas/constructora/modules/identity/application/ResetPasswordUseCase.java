package com.saas.constructora.modules.identity.application;

import com.saas.constructora.modules.identity.application.ports.PasswordResetTokenRepository;
import com.saas.constructora.modules.identity.application.ports.UserRepository;
import com.saas.constructora.modules.identity.domain.PasswordResetToken;
import com.saas.constructora.shared.security.TokenHasher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

/**
 * Caso de uso HU-13: consumir el token y fijar la nueva contraseña.
 * El token debe existir, no haber sido usado y no estar vencido.
 */
@Service
public class ResetPasswordUseCase {

    private static final String TOKEN_INVALIDO = "Token inválido o expirado";

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenHasher tokenHasher;

    public ResetPasswordUseCase(PasswordResetTokenRepository tokenRepository,
                                UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                TokenHasher tokenHasher) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenHasher = tokenHasher;
    }

    @Transactional
    public void reset(String rawToken, String newPassword) {
        PasswordResetToken token = tokenRepository.findByTokenHash(tokenHasher.hash(rawToken))
                .filter(t -> t.isValidAt(LocalDateTime.now()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, TOKEN_INVALIDO));

        userRepository.updatePasswordHash(token.getUserId(), passwordEncoder.encode(newPassword));
        tokenRepository.invalidateActiveTokensForUser(token.getUserId());
    }
}
