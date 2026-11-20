package com.ecogestion.security;

import com.ecogestion.service.UsuarioService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Le dice a Spring Security cómo cargar una cuenta a partir del correo con el que se intenta iniciar sesión.
 * Después Spring compara la contraseña escrita con el hash BCrypt guardado.
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Indirección:</b> conecta Spring Security con {@code UsuarioService} sin que el servicio dependa del framework.</li>
 *   <li><b>Fabricación pura:</b> clase técnica exigida por Spring Security; no es un concepto del caso de estudio.</li>
 * </ul>
 */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioService usuarioService;

    public UsuarioDetailsService(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Override
    public UserDetails loadUserByUsername(String correo) {
        return usuarioService.buscarPorCorreo(correo)
                .map(usuario -> new CuentaUsuario(usuario, LocalDateTime.now()))
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));
    }
}
