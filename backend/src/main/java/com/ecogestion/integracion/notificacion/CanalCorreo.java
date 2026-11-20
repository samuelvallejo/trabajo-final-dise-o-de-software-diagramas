package com.ecogestion.integracion.notificacion;

import com.ecogestion.config.EcoGestionProperties;
import com.ecogestion.model.Notificacion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Canal de correo electrónico (SMTP/TLS:587 en la vista de despliegue). Se activa con
 * {@code NOTIFICACIONES_CANAL=correo} y las variables SMTP_*.
 */
@Component
@ConditionalOnProperty(name = "ecogestion.notificaciones.canal", havingValue = "correo")
public class CanalCorreo implements CanalNotificacion {

    private final JavaMailSender mailSender;
    private final String remitente;

    public CanalCorreo(JavaMailSender mailSender, EcoGestionProperties propiedades) {
        this.mailSender = mailSender;
        this.remitente = propiedades.notificaciones().remitente();
    }

    @Override
    public String nombre() {
        return "correo";
    }

    @Override
    public void enviar(Notificacion notificacion) {
        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(notificacion.getCorreoDestino());
        mensaje.setSubject(notificacion.getAsunto());
        mensaje.setText(notificacion.getMensaje());
        try {
            mailSender.send(mensaje);
        } catch (MailException e) {
            throw new EnvioNotificacionException("No se pudo enviar el correo a " + notificacion.getCorreoDestino(), e);
        }
    }
}
