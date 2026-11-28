package com.SaaS.Constructora.identity.interfaces.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCompanyRequest(
        @NotBlank(message = "El nombre de la empresa es obligatorio")
        @Size(max = 100, message = "El nombre de la empresa no puede superar 100 caracteres")
        String name,
        @Size(max = 150, message = "El nombre comercial no puede superar 150 caracteres")
        String tradeName,
        @NotBlank(message = "El NIT es obligatorio")
        @Size(max = 50, message = "El NIT no puede superar 50 caracteres")
        String taxId,
        @Size(max = 50, message = "El teléfono no puede superar 50 caracteres")
        String contactPhone,
        @NotBlank(message = "La dirección es obligatoria")
        @Size(max = 255, message = "La dirección no puede superar 255 caracteres")
        String address,
        @Email(message = "El correo corporativo no tiene un formato válido")
        @Size(max = 255, message = "El correo corporativo no puede superar 255 caracteres")
        String companyEmail
) {}
