package com.saas.constructora.modules.identity.application;

import com.saas.constructora.modules.identity.application.ports.TenantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Puerta de entrada pública del módulo identity para que otros módulos
 * validen la existencia de una empresa sin acceder a su repositorio.
 */
@Service
public class TenantQueryService {

    private final TenantRepository tenantRepository;

    public TenantQueryService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public void ensureExists(UUID tenantId) {
        if (tenantId == null || tenantRepository.findById(tenantId).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "La empresa (tenant) asociada al token no existe");
        }
    }
}
