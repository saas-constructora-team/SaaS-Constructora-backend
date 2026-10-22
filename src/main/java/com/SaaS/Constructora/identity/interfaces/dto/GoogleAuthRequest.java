package com.SaaS.Constructora.identity.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * HU-01: entrada del inicio de sesión / registro con Google.
 * El ID token lo obtiene el frontend con Google Identity Services.
 * Si el correo no tiene empresa todavía, se envía {@code companyName} para
 * crear el tenant (registro abierto) y completar el alta.
 */
public record GoogleAuthRequest(
    @NotBlank(message = "El token de Google es obligatorio")
    String idToken,
    String companyName
) { }
