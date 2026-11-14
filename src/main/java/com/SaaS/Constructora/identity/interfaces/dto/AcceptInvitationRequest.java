package com.SaaS.Constructora.identity.interfaces.dto;

import com.SaaS.Constructora.shared.validation.PasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos para aceptar una invitación. La contraseña es opcional: solo se exige
 * cuando la cuenta se creó inactiva al invitar (email que no existía antes).
 */
public record AcceptInvitationRequest(
        @NotBlank(message = "El token es obligatorio") String token,

        @Size(max = 100, message = "La contraseña no puede superar los 100 caracteres")
        @Pattern(regexp = PasswordPolicy.PATTERN, message = PasswordPolicy.MESSAGE)
        String password
) {
}
