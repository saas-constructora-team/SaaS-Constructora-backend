package com.saas.constructora.modules.identity.application;

import com.saas.constructora.modules.identity.application.ports.TenantRepository;
import com.saas.constructora.modules.identity.application.ports.UserRepository;
import com.saas.constructora.modules.identity.domain.Role;
import com.saas.constructora.modules.identity.domain.Tenant;
import com.saas.constructora.modules.identity.domain.User;
import com.saas.constructora.modules.identity.interfaces.dto.RegisterTenantRequest;
import com.saas.constructora.modules.identity.interfaces.dto.RegisterTenantResponse;
import com.saas.constructora.shared.security.JwtService;
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
 * Crea tenant y admin en una sola transacción y devuelve sesión iniciada (JWT).
 */
@Service
public class RegisterTenantUseCase {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public RegisterTenantUseCase(TenantRepository tenantRepository, UserRepository userRepository,
                                 PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public RegisterTenantResponse register(RegisterTenantRequest request) {
        String email = request.adminEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El email ya está registrado");
        }

        String slug = Tenant.slugify(request.companyName());
        if (tenantRepository.existsBySlug(slug)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una empresa con ese nombre");
        }

        LocalDateTime now = LocalDateTime.now();
        Tenant tenant = tenantRepository.save(
                new Tenant(UUID.randomUUID(), request.companyName().trim(), slug, now));

        User admin = userRepository.save(new User(
                UUID.randomUUID(), tenant.getId(), email, request.adminFullName().trim(),
                passwordEncoder.encode(request.adminPassword()), Role.ADMIN, true, now));

        String token = jwtService.generateToken(admin);
        return RegisterTenantResponse.from(token, jwtService.getTokenTtlSeconds(), tenant, admin);
    }
}
