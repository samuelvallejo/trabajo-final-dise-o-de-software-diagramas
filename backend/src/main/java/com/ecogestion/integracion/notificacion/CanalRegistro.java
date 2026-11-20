package com.ecogestion.integracion.notificacion;

import com.ecogestion.model.Notificacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Canal por defecto cuando no hay servidor SMTP configurado: escribe la notificación en el log.
 * Permite probar el flujo de notificaciones sin credenciales de correo.
 */
@Component
@ConditionalOnProperty(name = "ecogestion.notificaciones.canal", havingValue = "registro", matchIfMissing = true)
public class CanalRegistro implements CanalNotificacion {

    private static final Logger LOG = LoggerFactory.getLogger(CanalRegistro.class);

    @Override
    public String nombre() {
        return "registro";
    }

    @Override
    public void enviar(Notificacion notificacion) {
        LOG.info("Notificación para {} | {} | {}", notificacion.getCorreoDestino(), notificacion.getAsunto(),
                notificacion.getMensaje());
    }
}
