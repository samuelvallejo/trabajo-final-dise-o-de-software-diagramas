package com.ecogestion.integracion.notificacion;

/** Fallo al entregar una notificación por un canal (por ejemplo, el servidor SMTP no responde). */
public class EnvioNotificacionException extends RuntimeException {

    public EnvioNotificacionException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
