package com.SaaS.Constructora.identity.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Interruptor de despliegue del flujo de invitaciones (flag
 * {@code app.invitation.enabled}).
 * <p>
 * Permite publicar el código antes de exponer los endpoints. Cuando está
 * deshabilitado se responde 404 para no revelar que la funcionalidad existe.
 */
@Component
public class InvitationGate {

    private final boolean enabled;

    public InvitationGate(@Value("${app.invitation.enabled:true}") boolean enabled) {
        this.enabled = enabled;
    }

    public void requireEnabled() {
        if (!enabled) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "La gestión de invitaciones no está habilitada");
        }
    }
}
