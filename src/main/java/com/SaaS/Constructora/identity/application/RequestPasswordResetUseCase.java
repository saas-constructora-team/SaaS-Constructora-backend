package com.SaaS.Constructora.identity.application;

import com.SaaS.Constructora.identity.application.ports.EmailSender;
import com.SaaS.Constructora.identity.application.ports.PasswordResetTokenRepository;
import com.SaaS.Constructora.identity.application.ports.UserRepository;
import com.SaaS.Constructora.identity.domain.PasswordResetToken;
import com.SaaS.Constructora.identity.domain.User;
import com.SaaS.Constructora.shared.security.TokenHasher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Caso de uso HU-13: solicitar el restablecimiento de contraseña.
 * Por seguridad NUNCA revela si el email existe: el endpoint responde igual en todos los casos.
 */
@Service
public class RequestPasswordResetUseCase {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailSender emailSender;
    private final TokenHasher tokenHasher;
    private final String baseUrl;
    private final long ttlMinutes;
    private final SecureRandom random = new SecureRandom();

    public RequestPasswordResetUseCase(UserRepository userRepository,
                                       PasswordResetTokenRepository tokenRepository,
                                       EmailSender emailSender,
                                       TokenHasher tokenHasher,
                                       @Value("${app.security.password-reset.base-url}") String baseUrl,
                                       @Value("${app.security.password-reset.ttl-minutes:30}") long ttlMinutes) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailSender = emailSender;
        this.tokenHasher = tokenHasher;
        this.baseUrl = baseUrl;
        this.ttlMinutes = ttlMinutes;
    }

    @Transactional
    public void request(String rawEmail) {
        String email = rawEmail.toLowerCase().trim();
        Optional<User> maybeUser = userRepository.findByEmail(email);

        // Si no existe o está inactivo, no se hace nada pero se responde igual (anti user-enumeration).
        if (maybeUser.isEmpty() || !maybeUser.get().canAuthenticate()) {
            return;
        }
        User user = maybeUser.get();

        tokenRepository.invalidateActiveTokensForUser(user.getId());

        String rawToken = generateRawToken();
        LocalDateTime now = LocalDateTime.now();
        tokenRepository.save(new PasswordResetToken(
                UUID.randomUUID(), user.getId(), tokenHasher.hash(rawToken),
                now.plusMinutes(ttlMinutes), false, now));

        String resetLink = baseUrl + "/reset-password?token=" + rawToken;
        emailSender.sendPasswordResetEmail(user.getEmail(), user.getFullName(), resetLink);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
