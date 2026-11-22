package com.SaaS.Constructora.identity.application.ports;

/**
 * Resultado de un intento de envío de correo.
 * <p>
 * Registrar una invitación (o un token de recuperación) no implica que el
 * mensaje haya salido del servidor. Este tipo permite a los casos de uso
 * distinguir ambos estados y exponerlos al cliente en lugar de asumir éxito.
 *
 * @param delivered          {@code true} cuando el proveedor aceptó el mensaje
 * @param providerMessageId  identificador devuelto por el proveedor (para verificar la entrega)
 * @param error              motivo del fallo, o {@code null} cuando se entregó
 */
public record EmailSendResult(boolean delivered, String providerMessageId, String error) {

    public static EmailSendResult delivered(String providerMessageId) {
        return new EmailSendResult(true, providerMessageId, null);
    }

    public static EmailSendResult failed(String error) {
        return new EmailSendResult(false, null, error == null ? "Error desconocido del proveedor de correo" : error);
    }

    public boolean isDelivered() {
        return delivered;
    }
}
