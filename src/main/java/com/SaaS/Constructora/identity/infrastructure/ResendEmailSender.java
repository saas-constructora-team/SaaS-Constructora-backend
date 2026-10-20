package com.SaaS.Constructora.identity.infrastructure;

import com.SaaS.Constructora.identity.application.ports.EmailSender;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Adaptador del puerto {@link EmailSender} que envía correos con Resend (HU-13).
 * Si no hay API key configurada (desarrollo local sin credenciales), registra
 * el enlace en el log en lugar de fallar, para no romper el flujo.
 */
@Component
class ResendEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailSender.class);

    private final String apiKey;
    private final String from;

    ResendEmailSender(@Value("${resend.api-key:}") String apiKey,
                      @Value("${resend.from}") String from) {
        this.apiKey = apiKey;
        this.from = from;
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String recipientName, String resetLink) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("RESEND_API_KEY no configurada. Enlace de recuperación para {}: {}", toEmail, resetLink);
            return;
        }

        send(toEmail, "Recuperación de contraseña", buildHtml(recipientName, resetLink));
    }

    @Override
    public void sendWelcomeEmail(String toEmail, String recipientName, String companyName) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("RESEND_API_KEY no configurada. Bienvenida pendiente para {}", toEmail);
            return;
        }

        send(toEmail, "¡Bienvenido a Constructora!",
                buildWelcomeHtml(recipientName, companyName));
    }

    private void send(String toEmail, String subject, String html) {
        Resend resend = new Resend(apiKey);
        CreateEmailOptions email = CreateEmailOptions.builder()
                .from(from)
                .to(toEmail)
                .subject(subject)
                .html(html)
                .build();

        try {
            CreateEmailResponse response = resend.emails().send(email);
            log.info("Correo \"{}\" enviado a {} (id={})", subject, toEmail, response.getId());
        } catch (ResendException e) {
            log.error("Fallo enviando correo \"{}\" a {}", subject, toEmail, e);
        }
    }

    private String buildWelcomeHtml(String recipientName, String companyName) {
        return """
                <p>Hola %s,</p>
                <p>Tu empresa <strong>%s</strong> ya está creada en Constructora.</p>
                <p>Puedes iniciar sesión en cualquier momento con tu cuenta de Google.</p>
                """.formatted(recipientName, companyName);
    }

    private String buildHtml(String recipientName, String resetLink) {
        return """
                <p>Hola %s,</p>
                <p>Recibimos una solicitud para restablecer tu contraseña.</p>
                <p><a href="%s">Restablecer contraseña</a></p>
                <p>Si no fuiste tú, ignora este correo. El enlace caduca en unos minutos.</p>
                """.formatted(recipientName, resetLink);
    }
}
