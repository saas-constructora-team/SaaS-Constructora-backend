package com.SaaS.Constructora.identity.infrastructure;

import com.SaaS.Constructora.identity.application.ports.GoogleTokenVerifier;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Adaptador del puerto {@link GoogleTokenVerifier}.
 * Valida el ID token de Google ("Sign in with Google") contra el endpoint
 * público de userinfo y, si se configuró {@code google.client-id}, comprueba
 * que el token haya sido emitido para ese cliente.
 *
 * Se eligió la validación vía endpoint (y no la firma local con JWKS) porque
 * el flujo simplificado de Google Identity Services emite el token sin firma
 * verificable en un endpoint de claves.
 */
@Component
class GoogleTokenVerifierClient implements GoogleTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(GoogleTokenVerifierClient.class);
    private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    private final RestClient restClient;
    private final String clientId;

    GoogleTokenVerifierClient(@Value("${google.client-id:}") String clientId) {
        this.restClient = RestClient.builder().baseUrl(USERINFO_URL).build();
        this.clientId = clientId;
    }

    @Override
    public GoogleAccount verify(String idToken) {
        JsonNode profile = fetchProfile(idToken);

        String subject = profile.path("sub").asText(null);
        String email = profile.path("email").asText(null);
        boolean emailVerified = profile.path("email_verified").asBoolean(false);
        String fullName = firstNonBlank(
                profile.path("name").asText(null),
                profile.path("given_name").asText(null),
                email);
        String audience = profile.path("aud").asText(null);

        if (!StringUtils.hasText(subject) || !StringUtils.hasText(email)) {
            throw unauthorized("El token de Google no tiene un perfil válido");
        }
        if (StringUtils.hasText(clientId) && !clientId.equals(audience)) {
            throw unauthorized("El token de Google fue emitido para otra aplicación");
        }

        return new GoogleAccount(subject, email.toLowerCase().trim(),
                fullName == null ? email : fullName.trim(), emailVerified);
    }

    private JsonNode fetchProfile(String idToken) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder.queryParam("id_token", idToken).build())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            log.warn("No se pudo verificar el ID token de Google: {}", e.getMessage());
            throw unauthorized("Token de Google inválido o expirado");
        }
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

    /** Utilidad de diagnóstico: expone los claims sin verificar la firma. No se usa en el flujo. */
    static JsonNode decodeClaimsUnsafe(String idToken) {
        try {
            String[] parts = idToken.split("\\.");
            byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(new String(decoded, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return null;
        }
    }
}
