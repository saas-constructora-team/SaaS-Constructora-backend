package com.SaaS.Constructora.identity.application;

import com.SaaS.Constructora.identity.application.ports.MembershipRepository;
import com.SaaS.Constructora.identity.application.ports.TenantRepository;
import com.SaaS.Constructora.identity.application.ports.UserRepository;
import com.SaaS.Constructora.identity.domain.Membership;
import com.SaaS.Constructora.identity.domain.Role;
import com.SaaS.Constructora.identity.domain.Tenant;
import com.SaaS.Constructora.identity.domain.User;
import com.SaaS.Constructora.shared.security.JwtService;
import com.SaaS.Constructora.shared.validation.PasswordPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Caso de uso HU-01: registro abierto de una empresa nueva (tenant) + su usuario administrador.
 * Validación contextual: email y slug únicos. Registro abierto: no requiere autenticación previa.
 * Crea tenant, admin y membership(ADMIN) en una sola transacción y devuelve sesión iniciada (JWT).
 */
@Service
public class RegisterTenantUseCase {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public RegisterTenantUseCase(TenantRepository tenantRepository, UserRepository userRepository,
                                 MembershipRepository membershipRepository,
                                 PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public RegisterTenantResult register(RegisterTenantCommand request) {
        if (!PasswordPolicy.isValid(request.adminPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, PasswordPolicy.MESSAGE);
        }

        String email = request.adminEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El email ya está registrado");
        }

        String slug = Tenant.slugify(request.companyName());
        if (tenantRepository.existsBySlug(slug)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una empresa con ese nombre");
        }

        LocalDateTime now = LocalDateTime.now();
        Tenant tenant = tenantRepository.save(new Tenant(
                UUID.randomUUID(), request.companyName().trim(), slug,
                normalize(request.tradeName()), normalize(request.taxId()),
                normalize(request.contactPhone()), normalize(request.address()),
                normalize(request.companyEmail()), now));

        User admin = userRepository.save(new User(
                UUID.randomUUID(), email, request.adminFullName().trim(),
                passwordEncoder.encode(request.adminPassword()), true, now));

        // Crear membresía ADMIN para el usuario en el tenant
        Membership membership = new Membership(
                UUID.randomUUID(), admin.getId(), tenant.getId(), Role.ADMIN, true, now);
        membershipRepository.save(membership);

        String token = jwtService.generateToken(admin, membership);
        return new RegisterTenantResult(token, jwtService.getTokenTtlSeconds(), tenant, admin, membership);
    }

    /** Normaliza los datos opcionales de la empresa: recorta y convierte vacíos en null. */
    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
