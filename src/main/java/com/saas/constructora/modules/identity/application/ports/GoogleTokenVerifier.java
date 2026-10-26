package com.saas.constructora.modules.identity.application.ports;

/**
 * Puerto del módulo identity para verificar identidades de Google.
 * La implementación real vive en infrastructure y valida el ID token
 * contra los endpoints públicos de Google (JWKS / userinfo).
 */
public interface GoogleTokenVerifier {

    /**
     * Verifica el ID token emitido por Google y devuelve los datos de la cuenta.
     *
     * @throws org.springframework.web.server.ResponseStatusException 401 si el token
     *         es inválido, vencido o fue emitido para otro cliente.
     */
    GoogleAccount verify(String idToken);

    /** Datos mínimos del perfil de Google que el caso de uso necesita. */
    record GoogleAccount(String subject, String email, String fullName, boolean emailVerified) { }
}
