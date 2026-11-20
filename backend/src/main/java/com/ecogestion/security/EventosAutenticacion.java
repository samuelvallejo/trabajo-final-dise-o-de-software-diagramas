package com.ecogestion.security;

import com.ecogestion.service.UsuarioService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Escucha los eventos de inicio de sesión de Spring Security: cuenta los intentos fallidos (al quinto la
 * cuenta se bloquea 15 minutos) y los reinicia cuando el ingreso es correcto (HU-01, criterio 5).
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Indirección:</b> separa el aviso de Spring Security de la decisión, que toma {@code UsuarioService}
 *       y, al final, el propio {@code Usuario}.</li>
 *   <li><b>Bajo acoplamiento:</b> {@code AuthController} no tiene que acordarse de contar intentos; ocurre siempre.</li>
 * </ul>
 */
@Component
public class EventosAutenticacion {

    private final UsuarioService usuarioService;

    public EventosAutenticacion(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @EventListener
    public void alFallar(AuthenticationFailureBadCredentialsEvent evento) {
        usuarioService.registrarIntentoFallido(evento.getAuthentication().getName());
    }

    @EventListener
    public void alIngresar(AuthenticationSuccessEvent evento) {
        usuarioService.registrarIngresoExitoso(evento.getAuthentication().getName());
    }
}
