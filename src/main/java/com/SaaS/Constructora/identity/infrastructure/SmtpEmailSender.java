package com.SaaS.Constructora.identity.infrastructure;

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
 *
 * Se activa con {@code app.email.provider=smtp}. Pensado para desarrollo con
 * Gmail (contraseña de aplicación) u otro servidor SMTP, ya que Resend exige
 * un dominio verificado para enviar a destinatarios arbitrarios.
 *
 * Los fallos de envío se registran y no se propagan: el flujo de recuperación
 * no debe revelar si el correo existe ni romperse por un problema del proveedor.
 */
@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "smtp")
class SmtpEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

    private final JavaMailSender mailSender;
    private final String from;

    SmtpEmailSender(JavaMailSender mailSender,
                    @Value("${app.email.from:${spring.mail.username:}}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void sendPasswordResetEmail(String toEmail, String recipientName, String resetLink) {
        send(toEmail, "Recuperación de contraseña", EmailTemplates.passwordReset(recipientName, resetLink));
    }

    @Override
    public void sendWelcomeEmail(String toEmail, String recipientName, String companyName) {
        send(toEmail, "¡Bienvenido a Constructora!", EmailTemplates.welcome(recipientName, companyName));
    }

    @Override
    public void sendInvitationEmail(String toEmail, String recipientName, String companyName,
                                    String inviteLink, long expiresInHours) {
        send(toEmail, "Invitación a " + companyName,
                EmailTemplates.invitation(recipientName, companyName, inviteLink, expiresInHours));
    }

    private void send(String toEmail, String subject, String html) {
        if (!StringUtils.hasText(from)) {
            log.error("MAIL_USERNAME/MAIL_FROM no configurado: no se puede enviar \"{}\" a {}", subject, toEmail);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(from);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Correo \"{}\" enviado a {} por SMTP", subject, toEmail);
        } catch (Exception e) {
            log.error("Fallo enviando correo \"{}\" a {} por SMTP: {}", subject, toEmail, e.getMessage());
        }
    }
}
