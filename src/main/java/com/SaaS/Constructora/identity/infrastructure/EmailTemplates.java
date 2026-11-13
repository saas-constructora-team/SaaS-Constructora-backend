package com.SaaS.Constructora.identity.infrastructure;

/** Plantillas HTML compartidas por los adaptadores de correo (HU-13). */
final class EmailTemplates {

    private EmailTemplates() {
    }

    static String passwordReset(String recipientName, String resetLink) {
        return """
                <p>Hola %s,</p>
                <p>Recibimos una solicitud para restablecer tu contraseña.</p>
                <p><a href="%s">Restablecer contraseña</a></p>
                <p>Si no fuiste tú, ignora este correo. El enlace caduca en unos minutos.</p>
                """.formatted(recipientName, resetLink);
    }

    static String welcome(String recipientName, String companyName) {
        return """
                <p>Hola %s,</p>
                <p>Tu empresa <strong>%s</strong> ya está creada en Constructora.</p>
                <p>Puedes iniciar sesión en cualquier momento con tu cuenta de Google.</p>
                """.formatted(recipientName, companyName);
    }

    static String invitation(String recipientName, String companyName, String inviteLink,
                             long expiresInHours) {
        return """
                <p>Hola %s,</p>
                <p><strong>%s</strong> te invitó a formar parte de su equipo en Constructora.</p>
                <p><a href="%s">Aceptar invitación</a></p>
                <p>El enlace caduca en %d horas. Si no esperabas esta invitación, ignora este correo.</p>
                <p>Si aún no tienes cuenta, en el mismo enlace podrás definir tu contraseña.</p>
                """.formatted(recipientName, companyName, inviteLink, expiresInHours);
    }
}
