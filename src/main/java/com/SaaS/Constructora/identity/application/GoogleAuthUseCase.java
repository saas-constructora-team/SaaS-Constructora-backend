package com.SaaS.Constructora.identity.application;

import com.SaaS.Constructora.identity.application.ports.EmailSender;
import com.SaaS.Constructora.identity.application.ports.ExternalIdentityRepository;
import com.SaaS.Constructora.identity.application.ports.GoogleTokenVerifier;
import com.SaaS.Constructora.identity.application.ports.MembershipRepository;
import com.SaaS.Constructora.identity.application.ports.TenantRepository;
import com.SaaS.Constructora.identity.application.ports.UserRepository;
import com.SaaS.Constructora.identity.domain.ExternalIdentity;
import com.SaaS.Constructora.identity.domain.Membership;
import com.SaaS.Constructora.identity.domain.Role;
import com.SaaS.Constructora.identity.domain.Tenant;
import com.SaaS.Constructora.identity.domain.User;
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
 * Validación contextual: el correo debe venir verificado por Google y, si se envía
 * un nombre de empresa, este no puede estar ya en uso. Cuando no se envía nombre,
 * se genera uno por defecto único para no chocar con otros usuarios de Google.
 */
@Service
public class GoogleAuthUseCase {

    private static final Logger log = LoggerFactory.getLogger(GoogleAuthUseCase.class);
    private static final int MAX_SLUG_ATTEMPTS = 5;

    private final GoogleTokenVerifier googleTokenVerifier;
    private final ExternalIdentityRepository externalIdentityRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;
    private final JwtService jwtService;
    private final SecureRandom random = new SecureRandom();

    public GoogleAuthUseCase(GoogleTokenVerifier googleTokenVerifier,
                             ExternalIdentityRepository externalIdentityRepository,
                             UserRepository userRepository,
                             MembershipRepository membershipRepository,
                             TenantRepository tenantRepository,
                             PasswordEncoder passwordEncoder,
                             EmailSender emailSender,
                             JwtService jwtService) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.externalIdentityRepository = externalIdentityRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailSender = emailSender;
        this.jwtService = jwtService;
    }

    @Transactional
    public GoogleAuthResult authenticate(GoogleAuthCommand request) {
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
            return session(user, requireActiveMembership(user), false);
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
            return session(user, requireActiveMembership(user), false);
        }

        // 3) Cuenta nueva -> registro abierto con Google
        requireVerifiedEmail(account);

        CompanyIdentity company = resolveCompany(request.companyName(), account.fullName());

        UUID userId = UUID.randomUUID();
        Tenant tenant = tenantRepository.save(
                new Tenant(UUID.randomUUID(), company.name(), company.slug(),
                        null, null, null, null, null, userId, now));

        // Contraseña aleatoria: la cuenta se gestiona con Google y no tiene credenciales propias.
        // Con HU-13 puede fijar una contraseña propia si algún día quiere entrar sin Google.
        String randomPassword = randomUrlSafeToken();
        User user = userRepository.save(new User(
                userId, account.email(), account.fullName(), null,
                passwordEncoder.encode(randomPassword), true, now));

        // Crear membresía ADMIN para el usuario en el tenant
        Membership membership = new Membership(
                UUID.randomUUID(), user.getId(), tenant.getId(), Role.ADMIN, true, now);
        membershipRepository.save(membership);

        externalIdentityRepository.save(ExternalIdentity.google(user.getId(), account.subject(), now));
        log.info("Cuenta creada con Google para {} en la empresa {}", user.getEmail(), tenant.getSlug());
        emailSender.sendWelcomeEmail(user.getEmail(), user.getFullName(), tenant.getName());

        return session(user, membership, true);
    }

    /**
     * Resuelve el nombre y el slug de la empresa.
     * - Si el usuario propuso un nombre, se respeta y un duplicado sigue siendo 409.
     * - Si falta (alta con Google), se genera "Empresa de {nombre}" y, si el slug
     *   ya existe, se agrega un sufijo corto aleatorio hasta encontrar uno libre.
     */
    private CompanyIdentity resolveCompany(String requestedName, String fullName) {
        if (StringUtils.hasText(requestedName)) {
            String name = requestedName.trim();
            String slug = Tenant.slugify(name);
            if (tenantRepository.existsBySlug(slug)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Ya existe una empresa con ese nombre; indique otro nombre o inicie sesión en la existente");
            }
            return new CompanyIdentity(name, slug);
        }

        String baseName = "Empresa de " + fullName;
        String baseSlug = Tenant.slugify(baseName);
        if (!StringUtils.hasText(baseSlug)) {
            baseSlug = "empresa";
        }
        if (!tenantRepository.existsBySlug(baseSlug)) {
            return new CompanyIdentity(baseName, baseSlug);
        }

        for (int attempt = 0; attempt < MAX_SLUG_ATTEMPTS; attempt++) {
            String suffix = randomSuffix();
            String candidateSlug = baseSlug + "-" + suffix;
            if (!tenantRepository.existsBySlug(candidateSlug)) {
                return new CompanyIdentity(baseName + " " + suffix, candidateSlug);
            }
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "No se pudo generar un nombre de empresa único; indique un nombre");
    }

    private String randomSuffix() {
        return String.format("%04x", random.nextInt(0x10000));
    }

    private record CompanyIdentity(String name, String slug) { }

    private void requireVerifiedEmail(GoogleTokenVerifier.GoogleAccount account) {
        // Solo se permite crear empresa con un correo que Google marque como verificado.
        // No hay excepciones por dominio (p. ej. @gmail.com).
        if (!account.emailVerified()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El correo de Google debe estar verificado para crear una empresa");
        }
    }

    private GoogleAuthResult session(User user, Membership membership, boolean newUser) {
        Tenant tenant = tenantRepository.findById(membership.getTenantId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "No se encontró la empresa del usuario"));
        String token = jwtService.generateToken(user, membership);
        return new GoogleAuthResult(token, jwtService.getTokenTtlSeconds(), newUser, tenant, user, membership);
    }

    /**
     * Obtiene la membresía activa del usuario para resolver el tenant y el rol auténticos.
     * Si el usuario pertenece a varios tenants, se toma la primera membresía activa.
     */
    private Membership requireActiveMembership(User user) {
        return membershipRepository.findActiveByUserId(user.getId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "El usuario no tiene membresía activa en ninguna empresa"));
    }

    private String randomUrlSafeToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
