package com.SaaS.Constructora.shared.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Los errores de infraestructura se registran, pero nunca se exponen en la API. */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void noExponeElTextoDeHikariCuandoElPoolSeAgota() {
        String interno = "HikariPool-7 - Connection is not available, request timed out after 30000ms";

        ResponseEntity<ApiError> response = handler.handleDatabaseUnavailable(
                new CannotGetJdbcConnectionException(interno, new SQLException(interno)));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("5", response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
        ApiError body = response.getBody();
        assertNotNull(body);
        assertEquals("SERVICE_UNAVAILABLE", body.code());
        assertFalse(body.message().contains("HikariPool"));
        assertFalse(body.message().contains("JDBC"));
        assertFalse(body.details().containsKey("message"));
    }

    @Test
    void respetaElResponseStatusDeLasExcepcionesDeDominio() {
        ResponseEntity<ApiError> response = handler.handleUnexpected(new FormatoNoAdmitido());

        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, response.getStatusCode());
        ApiError body = response.getBody();
        assertNotNull(body);
        assertEquals("UNSUPPORTED_MEDIA_TYPE", body.code());
        assertEquals("Formato de archivo no admitido", body.message());
    }

    @Test
    void losErroresNoControladosSeDevuelvenGenericos() {
        ResponseEntity<ApiError> response =
                handler.handleUnexpected(new IllegalStateException("secreto interno"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ApiError body = response.getBody();
        assertNotNull(body);
        assertEquals("INTERNAL_ERROR", body.code());
        assertFalse(body.message().contains("secreto interno"));
    }

    @ResponseStatus(value = HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            reason = "Formato de archivo no admitido")
    private static final class FormatoNoAdmitido extends RuntimeException { }
}
