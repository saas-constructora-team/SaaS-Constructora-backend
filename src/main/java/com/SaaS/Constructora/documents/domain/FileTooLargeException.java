package com.SaaS.Constructora.documents.domain;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** El archivo supera el límite configurado para su tipo (HTTP 413). */
@ResponseStatus(value = HttpStatus.PAYLOAD_TOO_LARGE, reason = "El archivo supera el tamaño permitido")
public class FileTooLargeException extends RuntimeException {

    public FileTooLargeException(String message) {
        super(message);
    }
}
