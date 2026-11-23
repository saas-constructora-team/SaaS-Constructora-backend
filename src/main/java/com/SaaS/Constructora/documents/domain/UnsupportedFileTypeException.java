package com.SaaS.Constructora.documents.domain;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Formato no incluido en la política de archivos del módulo (HTTP 415). */
@ResponseStatus(value = HttpStatus.UNSUPPORTED_MEDIA_TYPE, reason = "Formato de archivo no admitido")
public class UnsupportedFileTypeException extends RuntimeException {

    public UnsupportedFileTypeException(String message) {
        super(message);
    }
}
