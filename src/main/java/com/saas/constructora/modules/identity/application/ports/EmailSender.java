package com.saas.constructora.modules.identity.application.ports;

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
}
