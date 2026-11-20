package com.ecogestion.security;

import com.ecogestion.service.UsuarioService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Cuenta los intentos fallidos de inicio de sesión para bloquear la cuenta 15 minutos tras 5 fallos
 * seguidos, y los reinicia al entrar correctamente (HU-01, criterio 5).
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
