package com.SaaS.Constructora.identity.application.ports;

/**
 * Puerto de salida para el envío de correos (HU-13).
 * El caso de uso no conoce Resend: la implementación vive en infrastructure.
 */
public interface EmailSender {

    /** HU-01: correo de bienvenida tras crear la cuenta (hoy, cuenta creada con Google). */
    default void sendWelcomeEmail(String toEmail, String recipientName, String companyName) {
        // Sin implementación por defecto: el envío de bienvenida es opcional.
    }


    void sendPasswordResetEmail(String toEmail, String recipientName, String resetLink);

    /**
     * Correo con el enlace para aceptar una invitación a una empresa.
     * El remitente no conoce ni genera la contraseña: la persona la define al
     * consumir el token.
     */
    default void sendInvitationEmail(String toEmail, String recipientName, String companyName,
                                     String inviteLink, long expiresInHours) {
        // Sin implementación por defecto: el envío es opcional para adaptadores externos.
    }
}
