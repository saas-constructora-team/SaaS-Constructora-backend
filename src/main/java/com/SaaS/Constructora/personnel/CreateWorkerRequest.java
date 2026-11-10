package com.SaaS.Constructora.personnel;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Datos para registrar un trabajador en la empresa autenticada.
 * El email es opcional (personal operativo); el teléfono es obligatorio
 * porque se usa como contacto y para notificaciones.
 * El cargo se especifica por su nombre y debe existir en el catálogo del
 * tenant (o se crea automáticamente al registrar al trabajador).
 */
public record CreateWorkerRequest(

        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 150) String fullName,

        @NotNull(message = "El tipo de documento es obligatorio")
        DocumentType documentType,

        @NotBlank(message = "El número de documento es obligatorio")
        @Size(max = 50) String documentNumber,

        @NotBlank(message = "El teléfono móvil es obligatorio")
        @Size(max = 30) String phone,

        @Email(message = "El correo electrónico no tiene un formato válido")
        @Size(max = 255) String email,

        @NotBlank(message = "El nombre del contacto de emergencia es obligatorio")
        @Size(max = 150) String emergencyContactName,

        @NotBlank(message = "El teléfono del contacto de emergencia es obligatorio")
        @Size(max = 30) String emergencyContactPhone,

        /* Nombre del cargo del trabajador (catálogo dinámico por tenant). */
        @NotBlank(message = "El cargo del trabajador es obligatorio")
        @Size(max = 50) String position,

        /* Obra a la que se vincula el trabajador; opcional. */
        UUID projectId

) { }
