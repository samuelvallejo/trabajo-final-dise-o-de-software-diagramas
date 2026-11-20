package com.ecogestion.integracion.notificacion;

import com.ecogestion.model.Notificacion;

/**
 * Interfaz requerida "Canal de notificación" de la vista funcional (patrón Estrategia, HU-11).
 * Hoy existen el canal de correo SMTP y el canal de registro; se pueden agregar SMS o push
 * implementando esta interfaz sin modificar {@code NotificacionService}.
 */
public interface CanalNotificacion {

    /** Nombre del canal que queda guardado en la notificación. */
    String nombre();

    /**
     * Envía la notificación.
     *
     * @throws EnvioNotificacionException si el canal no pudo entregarla
     */
    void enviar(Notificacion notificacion);
}
