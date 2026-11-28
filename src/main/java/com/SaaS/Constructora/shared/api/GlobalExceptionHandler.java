package com.SaaS.Constructora.shared.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> details = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage() == null ? "Valor inválido" : error.getDefaultMessage(),
                        (first, ignored) -> first));
        return ResponseEntity.badRequest().body(ApiError.validation(details));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiError> handleStatus(ResponseStatusException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String message = exception.getReason() == null ? status.getReasonPhrase() : exception.getReason();
        return ResponseEntity.status(status)
                .body(ApiError.of(status.value(), status.getReasonPhrase(), message));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> handleConflict(DataIntegrityViolationException exception) {
        HttpStatus status = HttpStatus.CONFLICT;
        return ResponseEntity.status(status)
                .body(ApiError.of(status.value(), status.getReasonPhrase(),
                        "La operación entra en conflicto con un recurso existente", "CONFLICT"));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ApiError> handleConcurrentChange(OptimisticLockingFailureException exception) {
        HttpStatus status = HttpStatus.CONFLICT;
        return ResponseEntity.status(status)
                .body(ApiError.of(status.value(), status.getReasonPhrase(),
                        "Otra persona realizó un cambio antes; vuelve a cargar los datos", "CONFLICT"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> handleTooLarge(MaxUploadSizeExceededException exception) {
        HttpStatus status = HttpStatus.PAYLOAD_TOO_LARGE;
        return ResponseEntity.status(status).body(ApiError.of(status.value(),
                status.getReasonPhrase(), "El archivo supera el tamaño permitido", "PAYLOAD_TOO_LARGE"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> handleMissingResource(NoResourceFoundException exception) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        return ResponseEntity.status(status)
                .body(ApiError.of(status.value(), status.getReasonPhrase(),
                        "Recurso no encontrado", "NOT_FOUND"));
    }

    /**
     * Pool agotado, timeout de consulta o pérdida de conexión: se registra con
     * stacktrace en el servidor y se devuelve un 503 sin detalles internos.
     */
    @ExceptionHandler({DataAccessResourceFailureException.class, TransientDataAccessException.class})
    ResponseEntity<ApiError> handleDatabaseUnavailable(Exception exception) {
        log.error("No se pudo acceder a la base de datos", exception);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, "5")
                .body(ApiError.serviceUnavailable());
    }

    /** Red de seguridad: ningún error no controlado debe filtrar mensajes internos. */
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        // Spring ya mapea las excepciones de dominio anotadas con @ResponseStatus.
        // El handler genérico no debe absorberlas y convertirlas en 500.
        ResponseStatus annotated = AnnotationUtils.findAnnotation(exception.getClass(), ResponseStatus.class);
        if (annotated != null) {
            HttpStatus status = annotated.value();
            String message = annotated.reason().isBlank() ? status.getReasonPhrase() : annotated.reason();
            return ResponseEntity.status(status)
                    .body(ApiError.of(status.value(), status.getReasonPhrase(), message));
        }

        log.error("Error no controlado", exception);
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).body(ApiError.of(status.value(),
                status.getReasonPhrase(), "Ocurrió un error inesperado. Intenta de nuevo más tarde.",
                "INTERNAL_ERROR"));
    }
}
