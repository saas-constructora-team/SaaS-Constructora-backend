package com.SaaS.Constructora.identity.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifica que la validación del ID token falle cerrada cuando no hay
 * {@code google.client-id} configurado (no debe aceptar ningún token).
 */
class GoogleTokenVerifierClientTest {

    @Test
    void sinClientIdRechazaCualquierToken() {
        GoogleTokenVerifierClient client = new GoogleTokenVerifierClient("");

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> client.verify("un-token-cualquiera"));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
    }

    @Test
    void clientIdEnBlancoTambienFallaCerrado() {
        GoogleTokenVerifierClient client = new GoogleTokenVerifierClient("   ");

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> client.verify("un-token-cualquiera"));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatusCode());
    }
}
