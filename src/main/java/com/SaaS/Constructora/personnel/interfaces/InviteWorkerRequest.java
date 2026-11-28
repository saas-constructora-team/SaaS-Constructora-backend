package com.SaaS.Constructora.personnel.interfaces;
import com.SaaS.Constructora.personnel.domain.DocumentType;

import com.SaaS.Constructora.identity.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Datos para invitar a un trabajador a la empresa autenticada.
 * <p>
 * No incluye contraseña: la persona invitada define la suya al aceptar el
 * enlace. Si el correo ya pertenece a una cuenta existente, no se crea otro
 * usuario; solo se vincula una nueva membresía.
 */
public record InviteWorkerRequest(

        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 150) String fullName,

        @Size(min = 3, max = 30, message = "El username debe tener entre 3 y 30 caracteres")
        @jakarta.validation.constraints.Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El username solo puede contener letras, números, punto, guion y guion bajo")
        String username,

        @NotNull(message = "El tipo de documento es obligatorio")
        DocumentType documentType,

        @NotBlank(message = "El número de documento es obligatorio")
        @Size(max = 50) String documentNumber,

        @NotBlank(message = "El teléfono móvil es obligatorio")
        @Size(max = 30) String phone,

        @NotBlank(message = "El correo electrónico es obligatorio")
        @Email(message = "El correo electrónico no tiene un formato válido")
        @Size(max = 255) String email,

        @NotBlank(message = "El nombre del contacto de emergencia es obligatorio")
        @Size(max = 150) String emergencyContactName,

        @NotBlank(message = "El teléfono del contacto de emergencia es obligatorio")
        @Size(max = 30) String emergencyContactPhone,

        @NotBlank(message = "El cargo del trabajador es obligatorio")
        @Size(max = 50) String position,

        /* Obra a la que se vincula el trabajador; opcional. */
        UUID projectId,

        /* Rol dentro de la empresa: por defecto WORKER. No se permite invitar como ADMIN. */
        Role role
) {
}
