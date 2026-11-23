package com.SaaS.Constructora.documents.application.ports;

import java.io.InputStream;

/**
 * Puerto de almacenamiento de objetos. El módulo documents solo conoce esta
 * interfaz: el adaptador S3 es un detalle de infraestructura.
 */
public interface ObjectStoragePort {

    /** A partir de este tamaño se usa carga multipart para mayor fiabilidad. */
    long MULTIPART_THRESHOLD_BYTES = 100L * 1024 * 1024;

    /** Validez de las URLs firmadas de descarga (15 minutos). */
    long PRESIGN_TTL_SECONDS = 900L;

    /** Guarda el objeto bajo una clave generada por el servidor. */
    void store(String key, InputStream content, long size, String contentType);

    /** Devuelve una URL firmada de descarga con nombre de archivo forzado. */
    String presignDownload(String key, String originalName);

    /** Compensación cuando falla la persistencia de metadatos. */
    void delete(String key);
}
