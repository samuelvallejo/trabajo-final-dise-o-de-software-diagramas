package com.ecogestion.security;

import com.ecogestion.service.UsuarioService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/** Carga la cuenta por correo entre ciudadanos, operadores y administradores. */
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
