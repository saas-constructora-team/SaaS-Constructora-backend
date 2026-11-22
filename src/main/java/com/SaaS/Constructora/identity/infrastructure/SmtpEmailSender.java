package com.SaaS.Constructora.identity.infrastructure;

import com.SaaS.Constructora.identity.application.ports.EmailSendResult;
import com.SaaS.Constructora.identity.application.ports.EmailSender;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Adaptador del puerto {@link EmailSender} que envía correos por SMTP (HU-13).
 * <p>
 * Se activa con {@code app.email.provider=smtp}. Pensado para desarrollo con
 * Gmail (contraseña de aplicación) u otro servidor SMTP, ya que Resend exige
 * un dominio verificado para enviar a destinatarios arbitrarios.
 * <p>
 * Los fallos se devuelven como {@link EmailSendResult#failed(String)} con el
 * motivo y se registran con nivel ERROR; el flujo de recuperación sigue
 * respondiendo igual para no revelar si el correo existe (anti-enumeración).
 */
@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "smtp")
class SmtpEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

    private final JavaMailSender mailSender;
    private final String from;

    SmtpEmailSender(JavaMailSender mailSender,
                    @Value("${app.email.from:${spring.mail.username:}}") String from,
                    @Value("${spring.mail.host:}") String host) {
        this.mailSender = mailSender;
        this.from = from;
        log.info("[EMAIL] Proveedor activo=smtp, remitente={}, host={}", from, host);
        if (!StringUtils.hasText(from)) {
            log.error("[EMAIL] MAIL_FROM/MAIL_USERNAME está vacío. Los correos NO se enviarán.");
        }
    }

    @Override
    public EmailSendResult sendPasswordResetEmail(String toEmail, String recipientName, String resetLink) {
        return send(toEmail, "Recuperación de contraseña", EmailTemplates.passwordReset(recipientName, resetLink));
    }

    @Override
    public EmailSendResult sendWelcomeEmail(String toEmail, String recipientName, String companyName) {
        return send(toEmail, "¡Bienvenido a Constructora!", EmailTemplates.welcome(recipientName, companyName));
    }

    @Override
    public EmailSendResult sendInvitationEmail(String toEmail, String recipientName, String companyName,
                                               String inviteLink, long expiresInHours) {
        return send(toEmail, "Invitación a " + companyName,
                EmailTemplates.invitation(recipientName, companyName, inviteLink, expiresInHours));
    }

    private EmailSendResult send(String toEmail, String subject, String html) {
        if (!StringUtils.hasText(from) || !from.contains("@")) {
            String error = "MAIL_FROM/MAIL_USERNAME no configurado o inválido ('" + from + "')";
            log.error("[EMAIL] No se envió \"{}\" a {}: {}", subject, toEmail, error);
            return EmailSendResult.failed(error);
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(from);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("[EMAIL] Correo \"{}\" enviado a {} por SMTP", subject, toEmail);
            return EmailSendResult.delivered(null);
        } catch (Exception e) {
            String error = "Fallo enviando por SMTP: " + e.getMessage();
            log.error("[EMAIL] {}. Asunto=\"{}\", destinatario={}", error, subject, toEmail, e);
            return EmailSendResult.failed(error);
        }
    }
}
