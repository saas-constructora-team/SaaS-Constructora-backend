package com.SaaS.Constructora.identity.application;

import com.SaaS.Constructora.identity.application.ports.MembershipRepository;
import com.SaaS.Constructora.identity.application.ports.TenantRepository;
import com.SaaS.Constructora.identity.application.ports.UserRepository;
import com.SaaS.Constructora.identity.domain.Membership;
import com.SaaS.Constructora.identity.domain.Tenant;
import com.SaaS.Constructora.identity.domain.User;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AccountProfileUseCase {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9._-]{3,30}$");

    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final TenantRepository tenantRepository;

    public AccountProfileUseCase(UserRepository userRepository, MembershipRepository membershipRepository,
                                 TenantRepository tenantRepository) {
        this.userRepository = userRepository;
        this.membershipRepository = membershipRepository;
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public AccountProfile getProfile(UUID userId, UUID tenantId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        Membership membership = membershipRepository.findActiveByUserIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene acceso a este tenant"));
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));
        return AccountProfile.from(user, membership, tenant);
    }

    @Transactional
    public AccountProfile updateProfile(UUID userId, UUID tenantId, String fullName, String username) {
        AccountProfile current = getProfile(userId, tenantId);
        User user = current.user();

        String nextFullName = normalize(fullName);
        if (nextFullName == null) nextFullName = user.getFullName();
        if (nextFullName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre completo es obligatorio");
        }

        String nextUsername = User.normalizeUsername(username);
        if (username != null && username.trim().isEmpty()) {
            nextUsername = null; // permite limpiar username
        }
        if (nextUsername != null && !USERNAME_PATTERN.matcher(nextUsername).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El username debe tener entre 3 y 30 caracteres y solo puede contener letras minúsculas, números, punto, guion y guion bajo");
        }
        if (nextUsername != null && userRepository.existsByUsernameIgnoreCaseAndIdNot(nextUsername, userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El username ya está en uso");
        }

        User saved = userRepository.save(new User(user.getId(), user.getEmail(), nextFullName, nextUsername,
                user.getPasswordHash(), user.isActive(), user.getCreatedAt()));
        return AccountProfile.from(saved, current.membership(), current.tenant());
    }

    @Transactional
    public AccountProfile updateCompany(UUID userId, UUID tenantId, String name, String tradeName, String taxId,
                                        String contactPhone, String address, String companyEmail) {
        AccountProfile current = getProfile(userId, tenantId);
        if (!current.tenantOwner()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo el dueño de la empresa puede editar la información de la empresa");
        }
        Tenant tenant = current.tenant();
        String nextName = require(name, "El nombre de la empresa es obligatorio");
        String nextTaxId = require(taxId, "El NIT es obligatorio");
        String nextAddress = require(address, "La dirección es obligatoria");

        Tenant updated = tenant.updateCompanyProfile(nextName, normalize(tradeName), nextTaxId,
                normalize(contactPhone), nextAddress, normalize(companyEmail));
        Tenant saved = tenantRepository.save(updated);
        return AccountProfile.from(current.user(), current.membership(), saved);
    }

    private static String normalize(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String require(String value, String message) {
        String normalized = normalize(value);
        if (normalized == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        return normalized;
    }
}
