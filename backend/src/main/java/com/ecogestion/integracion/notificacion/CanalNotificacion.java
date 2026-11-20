package com.ecogestion.integracion.notificacion;

import com.ecogestion.model.Notificacion;

/**
 * Interfaz requerida "Canal de notificación" de la vista funcional (HU-11). Hoy existen el canal de correo
 * SMTP y el canal de registro; para SMS o notificaciones push basta con crear otra clase que la implemente.
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Polimorfismo:</b> {@code NotificacionService} llama a {@code enviar()} sin saber qué canal es.</li>
 *   <li><b>Variaciones protegidas:</b> agregar un canal nuevo no obliga a cambiar {@code NotificacionService}.</li>
 * </ul>
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
