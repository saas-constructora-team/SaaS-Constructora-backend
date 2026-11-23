package com.SaaS.Constructora.documents.interfaces;

import com.SaaS.Constructora.documents.application.AddVersionCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Metadatos de una nueva versión inmutable. */
public record AddVersionRequest(
        @NotBlank(message = "La etiqueta de versión es obligatoria")
        @Pattern(regexp = "[A-Za-z0-9._-]{1,20}",
                message = "La etiqueta solo admite letras, números, punto, guion y guion bajo")
        String versionLabel
) {
    public AddVersionCommand toCommand() {
        return new AddVersionCommand(versionLabel);
    }
}
