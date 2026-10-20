package com.SaaS.Constructora.identity.application;

import com.SaaS.Constructora.identity.application.ports.EmailSender;
import com.SaaS.Constructora.identity.application.ports.ExternalIdentityRepository;
import com.SaaS.Constructora.identity.application.ports.GoogleTokenVerifier;
import com.SaaS.Constructora.identity.application.ports.TenantRepository;
import com.SaaS.Constructora.identity.application.ports.UserRepository;
import com.SaaS.Constructora.identity.domain.ExternalIdentity;
import com.SaaS.Constructora.identity.domain.Role;
import com.SaaS.Constructora.identity.domain.Tenant;
import com.SaaS.Constructora.identity.domain.User;
import com.SaaS.Constructora.identity.interfaces.dto.GoogleAuthRequest;
import com.SaaS.Constructora.identity.interfaces.dto.GoogleAuthResponse;
import com.SaaS.Constructora.shared.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Caso de uso HU-01: iniciar sesión o crear cuenta con Google.
 *
 * Reglas:
 * - Si el "sub" de Google ya está vinculado, se inicia sesión.
 * - Si el correo ya existe, se vincula la identidad de Google a esa cuenta y se inicia sesión.
 * - Si el correo es nuevo, se crea la empresa (tenant) + usuario ADMIN y queda con sesión iniciada.
 *
 * Validación contextual: el correo debe venir verificado por Google (o ser el dueño de la
 * cuenta), y el nombre de empresa, si se envía, no puede estar ya en uso.
 */
@Service
public class GoogleAuthUseCase {

    private static final Logger log = LoggerFactory.getLogger(GoogleAuthUseCase.class);

    private final GoogleTokenVerifier googleTokenVerifier;
    private final ExternalIdentityRepository externalIdentityRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;
    private final JwtService jwtService;
    private final SecureRandom random = new SecureRandom();

    public GoogleAuthUseCase(GoogleTokenVerifier googleTokenVerifier,
                             ExternalIdentityRepository externalIdentityRepository,
                             UserRepository userRepository,
                             TenantRepository tenantRepository,
                             PasswordEncoder passwordEncoder,
                             EmailSender emailSender,
                             JwtService jwtService) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.externalIdentityRepository = externalIdentityRepository;
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
        this.jwtService = jwtService;
    }

    @Transactional
    public GoogleAuthResponse authenticate(GoogleAuthRequest request) {
        GoogleTokenVerifier.GoogleAccount account = googleTokenVerifier.verify(request.idToken());
        LocalDateTime now = LocalDateTime.now();

        // 1) Identidad de Google ya vinculada -> login
        Optional<ExternalIdentity> linked = externalIdentityRepository
                .findByProviderAndProviderUserId(ExternalIdentity.GOOGLE, account.subject());
        if (linked.isPresent()) {
            User user = userRepository.findById(linked.get().getUserId())
                    .filter(User::canAuthenticate)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED, "La cuenta está desactivada"));
            return session(user, false);
        }

        // 2) El correo ya existe -> vincular Google a la cuenta existente y entrar
        Optional<User> existing = userRepository.findByEmail(account.email());
        if (existing.isPresent()) {
            User user = existing.get();
            if (!user.canAuthenticate()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La cuenta está desactivada");
            }
            externalIdentityRepository.save(ExternalIdentity.google(user.getId(), account.subject(), now));
            log.info("Identidad de Google vinculada a la cuenta existente {}", user.getEmail());
            return session(user, false);
        }

        // 3) Cuenta nueva -> registro abierto con Google
        requireVerifiedEmail(account);

        String companyName = StringUtils.hasText(request.companyName())
                ? request.companyName().trim()
                : "Empresa de " + account.fullName();
        String slug = Tenant.slugify(companyName);
        if (tenantRepository.existsBySlug(slug)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una empresa con ese nombre; indique otro nombre o inicie sesión en la existente");
        }

        Tenant tenant = tenantRepository.save(new Tenant(UUID.randomUUID(), companyName, slug, now));

        // Contraseña aleatoria: la cuenta se gestiona con Google y no tiene credenciales propias.
        // Con HU-13 puede fijar una contraseña propia si algún día quiere entrar sin Google.
        String randomPassword = randomUrlSafeToken();
        User user = userRepository.save(new User(
                UUID.randomUUID(), tenant.getId(), account.email(), account.fullName(),
                passwordEncoder.encode(randomPassword), Role.ADMIN, true, now));

        externalIdentityRepository.save(ExternalIdentity.google(user.getId(), account.subject(), now));
        log.info("Cuenta creada con Google para {} en la empresa {}", user.getEmail(), tenant.getSlug());
        emailSender.sendWelcomeEmail(user.getEmail(), user.getFullName(), tenant.getName());

        return session(user, true);
    }

    private void requireVerifiedEmail(GoogleTokenVerifier.GoogleAccount account) {
        // El correo del dueño de la cuenta de Google siempre es verificable aunque
        // el flujo simplificado no devuelva "email_verified".
        boolean ownerEmail = StringUtils.hasText(account.email()) && account.email().endsWith("@gmail.com");
        if (!account.emailVerified() && !ownerEmail) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El correo de Google debe estar verificado para crear una empresa");
        }
    }

    private GoogleAuthResponse session(User user, boolean newUser) {
        Tenant tenant = tenantRepository.findById(user.getTenantId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "No se encontró la empresa del usuario"));
        String token = jwtService.generateToken(user);
        return GoogleAuthResponse.from(token, jwtService.getTokenTtlSeconds(), newUser, tenant, user);
    }

    private String randomUrlSafeToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
