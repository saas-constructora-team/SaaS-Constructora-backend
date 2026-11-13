package com.SaaS.Constructora.identity.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para aceptar una invitación. La contraseña es opcional: solo se exige
 * cuando la cuenta se creó inactiva al invitar (email que no existía antes).
 */
public record AcceptInvitationRequest(
        @NotBlank(message = "El token es obligatorio") String token,

        @Size(min = 8, max = 100,
                message = "La contraseña debe tener entre 8 y 100 caracteres")
        String password
) {
}
