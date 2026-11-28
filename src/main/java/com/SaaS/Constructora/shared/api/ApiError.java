package com.SaaS.Constructora.shared.api;

import java.time.Instant;
import java.util.Map;

public record ApiError(Instant timestamp, int status, String error, String message,
                       String code, Map<String, String> details) {

    public static ApiError of(int status, String error, String message) {
        return of(status, error, message, defaultCode(status));
    }

    public static ApiError of(int status, String error, String message, String code) {
        return new ApiError(Instant.now(), status, error, message, code, Map.of());
    }

    public static ApiError validation(Map<String, String> details) {
        return new ApiError(Instant.now(), 400, "Bad Request",
                "La solicitud contiene datos inválidos", "VALIDATION_ERROR", details);
    }

    /** Error de infraestructura temporal: el detalle se registra en el servidor, nunca se devuelve. */
    public static ApiError serviceUnavailable() {
        return of(503, "Service Unavailable",
                "El servicio no está disponible temporalmente. Intenta de nuevo en unos segundos.",
                "SERVICE_UNAVAILABLE");
    }

    private static String defaultCode(int status) {
        return switch (status) {
            case 400 -> "BAD_REQUEST";
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 409 -> "CONFLICT";
            case 413 -> "PAYLOAD_TOO_LARGE";
            case 415 -> "UNSUPPORTED_MEDIA_TYPE";
            case 500 -> "INTERNAL_ERROR";
            case 503 -> "SERVICE_UNAVAILABLE";
            default -> "REQUEST_FAILED";
        };
    }
}
