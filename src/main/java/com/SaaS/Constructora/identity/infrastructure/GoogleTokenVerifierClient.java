package com.SaaS.Constructora.identity.infrastructure;

import com.SaaS.Constructora.identity.application.ports.GoogleTokenVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Adaptador del puerto {@link GoogleTokenVerifier}.
 * Valida el ID token de Google ("Sign in with Google") contra el endpoint
 * público de tokeninfo y comprueba que el token haya sido emitido para este
 * cliente ({@code aud}) y por Google ({@code iss}).
 *
 * Se usa {@code tokeninfo?id_token=} (no {@code userinfo}) porque userinfo
 * espera un access token, no el ID token que emite Google Identity Services.
 *
 * La validación falla cerrada: si no hay {@code google.client-id} configurado,
 * ningún token se acepta.
 */
@Component
class GoogleTokenVerifierClient implements GoogleTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(GoogleTokenVerifierClient.class);
    private static final String TOKENINFO_URL = "https://oauth2.googleapis.com";
    private static final java.util.Set<String> VALID_ISSUERS =
            java.util.Set.of("accounts.google.com", "https://accounts.google.com");

    private final RestClient restClient;
    private final String clientId;

    GoogleTokenVerifierClient(@Value("${google.client-id:}") String clientId) {
        this.restClient = RestClient.builder().baseUrl(TOKENINFO_URL).build();
        this.clientId = clientId;
    }

    @Override
    public GoogleAccount verify(String idToken) {
        // Fail closed: sin client-id configurado no hay forma de validar el
        // audience, así que se rechaza cualquier token.
        if (!StringUtils.hasText(clientId)) {
            log.error("GOOGLE_CLIENT_ID no está configurado: se rechazan todos los inicios de sesión con Google");
            throw unauthorized("El inicio de sesión con Google no está configurado");
        }

        Map<String, Object> profile = fetchProfile(idToken);

        String subject = text(profile, "sub");
        String email = text(profile, "email");
        boolean emailVerified = flag(profile, "email_verified");
        String fullName = firstNonBlank(
                text(profile, "name"),
                text(profile, "given_name"),
                email);
        String audience = text(profile, "aud");
        String issuer = text(profile, "iss");

        if (!StringUtils.hasText(subject) || !StringUtils.hasText(email)) {
            throw unauthorized("El token de Google no tiene un perfil válido");
        }
        if (!clientId.equals(audience)) {
            throw unauthorized("El token de Google fue emitido para otra aplicación");
        }
        if (!VALID_ISSUERS.contains(issuer)) {
            throw unauthorized("El token de Google no fue emitido por Google");
        }

        return new GoogleAccount(subject, email.toLowerCase().trim(),
                fullName == null ? email : fullName.trim(), emailVerified);
    }

    private Map<String, Object> fetchProfile(String idToken) {
        try {
            Map<String, Object> profile = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/tokeninfo")
                            .queryParam("id_token", idToken)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() { });
            if (profile == null) {
                throw unauthorized("Token de Google inválido o expirado");
            }
            return profile;
        } catch (RestClientResponseException e) {
            // No se registra el token; solo el motivo que devuelve Google.
            log.warn("Google rechazó el ID token (HTTP {}): {}",
                    e.getStatusCode().value(), e.getResponseBodyAsString());
            throw unauthorized("Token de Google inválido o expirado");
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.warn("No se pudo verificar el ID token de Google: {}", e.getMessage());
            throw unauthorized("Token de Google inválido o expirado");
        }
    }

    private static String text(Map<String, Object> profile, String key) {
        Object value = profile.get(key);
        return value == null ? null : value.toString();
    }

    /** tokeninfo devuelve email_verified como texto ("true"/"false") o como booleano. */
    private static boolean flag(Map<String, Object> profile, String key) {
        Object value = profile.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        return value != null && Boolean.parseBoolean(value.toString());
    }

    private static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static ResponseStatusException unauthorized(String message) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
    }
}
