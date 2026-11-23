package com.SaaS.Constructora.documents;

import com.SaaS.Constructora.documents.domain.AssetKind;
import com.SaaS.Constructora.documents.domain.FilePolicy;
import com.SaaS.Constructora.documents.domain.FileTooLargeException;
import com.SaaS.Constructora.documents.domain.UnsupportedFileTypeException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FilePolicyTest {

    private static final byte[] PNG = new byte[]{
            (byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 1, 2, 3, 4, 5, 6, 7, 8
    };
    private static final byte[] PDF = "%PDF-1.7 contenido".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] GLB = "glTFbinario".getBytes(StandardCharsets.US_ASCII);

    @Test
    void aceptaUnaImagenConLaFirmaCorrecta() {
        assertDoesNotThrow(() -> FilePolicy.validate(AssetKind.IMAGE, "planta.png",
                "image/png", PNG.length, PNG));
    }

    @Test
    void rechazaUnEjecutableRenombradoAPng() {
        byte[] executable = new byte[]{'M', 'Z', (byte) 0x90, 0, 'e', 'j', 'e', 'c', 'u', 't', 'a', 'b', 'l', 'e'};
        assertThrows(UnsupportedFileTypeException.class,
                () -> FilePolicy.validate(AssetKind.IMAGE, "virus.png",
                        "image/png", executable.length, executable));
    }

    @Test
    void rechazaUnaExtensionNoAdmitidaParaElTipo() {
        assertThrows(UnsupportedFileTypeException.class,
                () -> FilePolicy.validate(AssetKind.IMAGE, "plano.pdf",
                        "application/pdf", PDF.length, PDF));
    }

    @Test
    void rechazaUnArchivoQueSuperaElLimiteDelTipo() {
        assertThrows(FileTooLargeException.class,
                () -> FilePolicy.validate(AssetKind.IMAGE, "planta.png",
                        "image/png", FilePolicy.MAX_IMAGE_BYTES + 1, PNG));
    }

    @Test
    void aceptaFormatosDeTextoSinFirmaBinaria() {
        byte[] gltf = "{\"asset\":{\"version\":\"2.0\"}}".getBytes(StandardCharsets.US_ASCII);
        assertDoesNotThrow(() -> FilePolicy.validate(AssetKind.RENDER_3D, "modelo.gltf",
                "model/gltf+json", gltf.length, gltf));
    }

    @Test
    void aceptaUnRenderGlbConSuFirma() {
        assertDoesNotThrow(() -> FilePolicy.validate(AssetKind.RENDER_3D, "fachada.glb",
                "model/gltf-binary", GLB.length, GLB));
    }
}
