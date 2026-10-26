package com.saas.constructora.modules.identity.interfaces.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterTenantRequest(
    @NotBlank(message = "El nombre de la empresa es obligatorio")
    @Size(max = 100) String companyName,
    @NotBlank(message = "El nombre del administrador es obligatorio")
    @Size(max = 150) String adminFullName,
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    @Size(max = 255) String adminEmail,
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    String adminPassword
) { }
