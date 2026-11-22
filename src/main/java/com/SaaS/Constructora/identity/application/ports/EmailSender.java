package com.SaaS.Constructora.identity.application.ports;

/**
 * Puerto de salida para el envío de correos (HU-13).
 * El caso de uso no conoce Resend: la implementación vive en infrastructure.
 * <p>
 * Los métodos devuelven {@link EmailSendResult} en lugar de {@code void} para
 * que el llamador pueda diferenciar "registro creado" de "correo enviado".
 * Ningún adaptador debe callar un fallo: si no se pudo entregar, se devuelve
 * {@link EmailSendResult#failed(String)} con el motivo.
 */
public interface EmailSender {

    /** HU-01: correo de bienvenida tras crear la cuenta (hoy, cuenta creada con Google). */
    default EmailSendResult sendWelcomeEmail(String toEmail, String recipientName, String companyName) {
        return EmailSendResult.failed("El adaptador de correo no implementa el envío de bienvenida");
    }

    EmailSendResult sendPasswordResetEmail(String toEmail, String recipientName, String resetLink);

    /**
     * Correo con el enlace para aceptar una invitación a una empresa.
     * El remitente no conoce ni genera la contraseña: la persona la define al
     * consumir el token.
     */
    default EmailSendResult sendInvitationEmail(String toEmail, String recipientName, String companyName,
                                                String inviteLink, long expiresInHours) {
        return EmailSendResult.failed("El adaptador de correo no implementa el envío de invitaciones");
    }
}
