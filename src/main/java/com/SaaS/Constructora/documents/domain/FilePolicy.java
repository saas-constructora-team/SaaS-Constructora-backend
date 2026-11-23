package com.SaaS.Constructora.documents.domain;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Reglas puras de dominio para archivos técnicos: extensiones admitidas,
 * tamaño máximo por tipo y comprobación de la firma binaria (magic bytes).
 * La extensión por sí sola no es suficiente: evita que un ejecutable renombrado
 * a {@code .png} llegue al repositorio centralizado.
 */
public final class FilePolicy {

    public static final long MAX_IMAGE_BYTES = 20L * 1024 * 1024;
    public static final long MAX_DOCUMENT_BYTES = 50L * 1024 * 1024;
    public static final long MAX_RENDER_BYTES = 500L * 1024 * 1024;

    private static final Map<AssetKind, Set<String>> ALLOWED_EXTENSIONS = Map.of(
            AssetKind.IMAGE, Set.of("png", "jpg", "jpeg", "webp"),
            AssetKind.DOCUMENT, Set.of("pdf", "doc", "docx", "xls", "xlsx", "csv", "txt"),
            AssetKind.RENDER_3D, Set.of("glb", "gltf", "obj", "fbx", "zip")
    );

    private static final byte[] PNG = hex("89504e470d0a1a0a");
    private static final byte[] JPEG = hex("ffd8ff");
    private static final byte[] PDF = "%PDF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] ZIP = hex("504b0304");
    private static final byte[] OLE2 = hex("d0cf11e0");
    private static final byte[] GLB = "glTF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] FBX = "Kaydara FBX Binary".getBytes(StandardCharsets.US_ASCII);

    private FilePolicy() { }

    /** Valida metadatos y la cabecera ya leída del archivo (mínimo 12 bytes cuando los hay). */
    public static void validate(AssetKind kind, String originalName, String contentType,
                                long size, byte[] header) {
        if (kind == null) {
            throw new UnsupportedFileTypeException("Debe indicar el tipo de archivo");
        }
        if (size <= 0) {
            throw new UnsupportedFileTypeException("El archivo está vacío");
        }
        long max = maxBytes(kind);
        if (size > max) {
            throw new FileTooLargeException("El tamaño máximo para " + kind + " es de "
                    + (max / (1024 * 1024)) + " MB");
        }

        String extension = extension(originalName);
        Set<String> allowed = ALLOWED_EXTENSIONS.get(kind);
        if (extension == null || !allowed.contains(extension)) {
            throw new UnsupportedFileTypeException("Extensión no admitida para " + kind);
        }
        validateSignature(extension, header == null ? new byte[0] : header);
    }

    /** Byte límite a partir del cual se usa carga multipart en el almacenamiento. */
    public static long maxBytes(AssetKind kind) {
        return switch (kind) {
            case IMAGE -> MAX_IMAGE_BYTES;
            case DOCUMENT -> MAX_DOCUMENT_BYTES;
            case RENDER_3D -> MAX_RENDER_BYTES;
        };
    }

    private static void validateSignature(String extension, byte[] header) {
        switch (extension) {
            case "png" -> require(header, PNG, extension);
            case "jpg", "jpeg" -> require(header, JPEG, extension);
            case "webp" -> {
                if (header.length < 12 || !startsWith(header, "RIFF".getBytes(StandardCharsets.US_ASCII))
                        || !"WEBP".equals(new String(header, 8, 4, StandardCharsets.US_ASCII))) {
                    throw new UnsupportedFileTypeException("La firma del archivo no corresponde a webp");
                }
            }
            case "pdf" -> require(header, PDF, extension);
            case "docx", "xlsx", "zip" -> require(header, ZIP, extension);
            case "doc", "xls" -> require(header, OLE2, extension);
            case "glb" -> require(header, GLB, extension);
            case "fbx" -> require(header, FBX, extension);
            case "gltf", "obj", "csv", "txt" -> {
                // Formatos de texto: no tienen una firma binaria fiable.
            }
            default -> throw new UnsupportedFileTypeException("Extensión no admitida: " + extension);
        }
    }

    private static void require(byte[] header, byte[] signature, String extension) {
        if (!startsWith(header, signature)) {
            throw new UnsupportedFileTypeException("La firma del archivo no corresponde a " + extension);
        }
    }

    private static boolean startsWith(byte[] header, byte[] signature) {
        if (header.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (header[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private static String extension(String originalName) {
        if (originalName == null) {
            return null;
        }
        int dot = originalName.lastIndexOf('.');
        if (dot < 0 || dot == originalName.length() - 1) {
            return null;
        }
        return originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static byte[] hex(String value) {
        byte[] result = new byte[value.length() / 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) Integer.parseInt(value.substring(i * 2, i * 2 + 2), 16);
        }
        return result;
    }
}
