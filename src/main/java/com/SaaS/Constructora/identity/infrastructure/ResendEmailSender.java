package com.SaaS.Constructora.identity.infrastructure;

import com.SaaS.Constructora.identity.application.ports.EmailSender;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Adaptador del puerto {@link EmailSender} que envía correos con Resend (HU-13).
 * Si no hay API key configurada (desarrollo local sin credenciales), registra
 * el enlace en el log en lugar de fallar, para no romper el flujo.
 *
 * Es el proveedor por defecto; se desactiva con {@code app.email.provider=smtp}.
 */
@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "resend", matchIfMissing = true)
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

        send(toEmail, "Recuperación de contraseña", EmailTemplates.passwordReset(recipientName, resetLink));
    }

    @Override
    public void sendWelcomeEmail(String toEmail, String recipientName, String companyName) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("RESEND_API_KEY no configurada. Bienvenida pendiente para {}", toEmail);
            return;
        }

        send(toEmail, "¡Bienvenido a Constructora!",
                EmailTemplates.welcome(recipientName, companyName));
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
}
