package com.SaaS.Constructora.documents.interfaces;

import com.SaaS.Constructora.documents.application.UploadDocumentCommand;
import com.SaaS.Constructora.documents.domain.AssetKind;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Metadatos enviados como parte JSON junto al archivo. */
public record UploadDocumentRequest(
        @NotBlank(message = "El título es obligatorio")
        @Pattern(regexp = ".{1,200}", message = "El título es demasiado largo")
        String title,

        @NotNull(message = "El tipo de archivo es obligatorio")
        AssetKind kind,

        @NotBlank(message = "La etiqueta de versión es obligatoria")
        @Pattern(regexp = "[A-Za-z0-9._-]{1,20}",
                message = "La etiqueta solo admite letras, números, punto, guion y guion bajo")
        String versionLabel
) {
    public UploadDocumentCommand toCommand() {
        return new UploadDocumentCommand(title, kind, versionLabel);
    }
}
