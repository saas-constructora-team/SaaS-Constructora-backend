package com.SaaS.Constructora.identity.interfaces.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterTenantRequest(
    @NotBlank(message = "El nombre de la empresa es obligatorio")
    @Size(max = 100) String companyName,
    @Size(max = 150) String tradeName,
    @Size(max = 50) String taxId,
    @Size(max = 50) String contactPhone,
    @Size(max = 255) String address,
    @Email(message = "El correo corporativo no tiene un formato válido")
    @Size(max = 255) String companyEmail,
    @NotBlank(message = "El nombre del administrador es obligatorio")
    @Size(max = 150) String adminFullName,
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    @Size(max = 255) String adminEmail,
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    String adminPassword
) { }
