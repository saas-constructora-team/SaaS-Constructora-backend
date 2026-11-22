package com.SaaS.Constructora.identity.infrastructure;

import com.SaaS.Constructora.identity.application.ports.EmailSendResult;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/** Verifica que un remitente SMTP no configurado se informe, no se silencie. */
class SmtpEmailSenderTest {

    @Test
    void remitenteVacioDevuelveFalloConMotivo() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        SmtpEmailSender sender = new SmtpEmailSender(mailSender, "", "smtp.gmail.com");

        EmailSendResult result = sender.sendInvitationEmail(
                "obrero@obra.com", "Obrero", "Constructora", "http://localhost:4200/invitations/accept?token=abc", 72);

        assertFalse(result.isDelivered());
        assertNotNull(result.error());
        assertTrue(result.error().contains("MAIL_FROM"));
    }
}
