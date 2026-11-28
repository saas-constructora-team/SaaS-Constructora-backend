package com.SaaS.Constructora.identity.interfaces.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateAccountProfileRequest(
        @Size(max = 150, message = "El nombre completo no puede superar 150 caracteres")
        String fullName,

        @Size(min = 3, max = 30, message = "El username debe tener entre 3 y 30 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El username solo puede contener letras, números, punto, guion y guion bajo")
        String username
) {}
