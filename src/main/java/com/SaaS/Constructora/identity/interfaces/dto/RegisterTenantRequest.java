package com.SaaS.Constructora.identity.interfaces.dto;

import com.SaaS.Constructora.shared.validation.PasswordPolicy;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterTenantRequest(
    @NotBlank(message = "El nombre de la empresa es obligatorio")
    @Size(max = 100) String companyName,
    @Size(max = 150) String tradeName,
    @NotBlank(message = "El NIT es obligatorio")
    @Size(max = 50) String taxId,
    @Size(max = 50) String contactPhone,
    @NotBlank(message = "La dirección es obligatoria")
    @Size(max = 255) String address,
    @Email(message = "El correo corporativo no tiene un formato válido")
    @Size(max = 255) String companyEmail,
    @NotBlank(message = "El nombre del administrador es obligatorio")
    @Size(max = 150) String adminFullName,
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    @Size(max = 255) String adminEmail,
    @NotBlank(message = "La contraseña es obligatoria")
    @Pattern(regexp = PasswordPolicy.PATTERN, message = PasswordPolicy.MESSAGE)
    String adminPassword
) { }
