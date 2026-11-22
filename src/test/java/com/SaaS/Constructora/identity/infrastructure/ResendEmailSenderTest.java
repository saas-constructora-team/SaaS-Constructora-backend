package com.SaaS.Constructora.identity.infrastructure;

import com.SaaS.Constructora.identity.application.ports.EmailSendResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifica que un problema de configuración de Resend se informe, no se silencie. */
class ResendEmailSenderTest {

    @Test
    void apiKeyVaciaDevuelveFalloConMotivo() {
        ResendEmailSender sender = new ResendEmailSender("", "onboarding@resend.dev");

        EmailSendResult result = sender.sendInvitationEmail(
                "obrero@obra.com", "Obrero", "Constructora", "http://localhost:4200/invitations/accept?token=abc", 72);

        assertFalse(result.isDelivered());
        assertNotNull(result.error());
        assertTrue(result.error().contains("RESEND_API_KEY"));
        assertEquals(null, result.providerMessageId());
    }

    @Test
    void remitenteInvalidoDevuelveFalloConMotivo() {
        ResendEmailSender sender = new ResendEmailSender("re_test", "saasconstructora.correounivalle.edu.co");

        EmailSendResult result = sender.sendPasswordResetEmail(
                "obrero@obra.com", "Obrero", "http://localhost:4200/reset-password?token=abc");

        assertFalse(result.isDelivered());
        assertNotNull(result.error());
        assertTrue(result.error().contains("RESEND_FROM"));
    }
}
