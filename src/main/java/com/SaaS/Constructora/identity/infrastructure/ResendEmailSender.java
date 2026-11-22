package com.SaaS.Constructora.identity.infrastructure;

import com.SaaS.Constructora.identity.application.ports.EmailSendResult;
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
import org.springframework.util.StringUtils;

/**
 * Adaptador del puerto {@link EmailSender} que envía correos con Resend (HU-13).
 * <p>
 * Es el proveedor por defecto; se desactiva con {@code app.email.provider=smtp}.
 * Un error de configuración o un rechazo de la API se devuelve como
 * {@link EmailSendResult#failed(String)} y se registra con nivel ERROR: nunca se
 * finge que el correo fue enviado.
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
        log.info("[EMAIL] Proveedor activo=resend, remitente={}, apiKeyConfigurada={}, apiKeyLongitud={}",
                from, StringUtils.hasText(apiKey), apiKey == null ? 0 : apiKey.trim().length());
        if (!StringUtils.hasText(apiKey)) {
            log.error("[EMAIL] RESEND_API_KEY está vacía. Los correos NO se enviarán. "
                    + "Define RESEND_API_KEY en .env o como variable de entorno y reinicia la aplicación.");
        }
    }

    @Override
    public EmailSendResult sendPasswordResetEmail(String toEmail, String recipientName, String resetLink) {
        return send(toEmail, "Recuperación de contraseña", EmailTemplates.passwordReset(recipientName, resetLink));
    }

    @Override
    public EmailSendResult sendWelcomeEmail(String toEmail, String recipientName, String companyName) {
        return send(toEmail, "¡Bienvenido a Constructora!",
                EmailTemplates.welcome(recipientName, companyName));
    }

    @Override
    public EmailSendResult sendInvitationEmail(String toEmail, String recipientName, String companyName,
                                               String inviteLink, long expiresInHours) {
        return send(toEmail, "Invitación a " + companyName,
                EmailTemplates.invitation(recipientName, companyName, inviteLink, expiresInHours));
    }

    private EmailSendResult send(String toEmail, String subject, String html) {
        String configError = validateConfiguration();
        if (configError != null) {
            log.error("[EMAIL] No se envió \"{}\" a {}: {}", subject, toEmail, configError);
            return EmailSendResult.failed(configError);
        }

        try {
            Resend resend = new Resend(apiKey);
            CreateEmailOptions email = CreateEmailOptions.builder()
                    .from(from)
                    .to(toEmail)
                    .subject(subject)
                    .html(html)
                    .build();

            CreateEmailResponse response = resend.emails().send(email);
            String messageId = response == null ? null : response.getId();
            log.info("[EMAIL] Correo \"{}\" enviado a {} por Resend (id={})", subject, toEmail, messageId);
            return EmailSendResult.delivered(messageId);
        } catch (ResendException e) {
            String error = "Resend rechazó el envío: " + e.getMessage();
            log.error("[EMAIL] {}. Asunto=\"{}\", destinatario={}", error, subject, toEmail, e);
            return EmailSendResult.failed(error);
        } catch (RuntimeException e) {
            String error = "Fallo inesperado al enviar por Resend: " + e.getMessage();
            log.error("[EMAIL] {}. Asunto=\"{}\", destinatario={}", error, subject, toEmail, e);
            return EmailSendResult.failed(error);
        }
    }

    /**
     * Devuelve el motivo por el que no se puede enviar, o {@code null} si la
     * configuración es válida. El remitente de Resend debe ser un dominio
     * verificado (o onboarding@resend.dev en pruebas).
     */
    private String validateConfiguration() {
        if (!StringUtils.hasText(apiKey)) {
            return "RESEND_API_KEY no configurada (app.email.provider=resend)";
        }
        if (!StringUtils.hasText(from) || !from.contains("@")) {
            return "RESEND_FROM inválido ('" + from + "'): use un correo de un dominio verificado en Resend";
        }
        return null;
    }
}
