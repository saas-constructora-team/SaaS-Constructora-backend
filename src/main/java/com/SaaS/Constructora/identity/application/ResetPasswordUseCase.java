package com.SaaS.Constructora.identity.application;

import com.SaaS.Constructora.identity.application.ports.PasswordResetTokenRepository;
import com.SaaS.Constructora.identity.application.ports.UserRepository;
import com.SaaS.Constructora.identity.domain.PasswordResetToken;
import com.SaaS.Constructora.identity.domain.User;
import com.SaaS.Constructora.shared.security.TokenHasher;
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

        // Misma regla que login y Google: una cuenta desactivada no puede operar.
        User user = userRepository.findById(token.getUserId())
                .filter(User::canAuthenticate)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "La cuenta está desactivada"));

        userRepository.updatePasswordHash(user.getId(), passwordEncoder.encode(newPassword));
        tokenRepository.invalidateActiveTokensForUser(user.getId());
    }
}
