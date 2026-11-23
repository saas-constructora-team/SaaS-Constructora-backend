package com.SaaS.Constructora.documents.application.ports;

import java.io.InputStream;

/**
 * Contenido de archivo entregado por la capa HTTP. El servicio es responsable
 * de cerrar el stream. Mantenerlo aquí evita que la aplicación dependa de
 * {@code MultipartFile} (API web).
 */
public record FileUpload(InputStream content, String originalName, String contentType, long size) { }
