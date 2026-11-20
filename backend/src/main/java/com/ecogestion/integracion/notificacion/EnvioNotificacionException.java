package com.ecogestion.integracion.notificacion;

/**
 * Error al entregar una notificación por un canal (por ejemplo, el servidor SMTP no responde).
 * {@code NotificacionService} la captura para reintentar.
 */
public class EnvioNotificacionException extends RuntimeException {

    public EnvioNotificacionException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
