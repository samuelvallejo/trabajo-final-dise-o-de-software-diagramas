package com.ecogestion.controller;

import com.ecogestion.dto.ContenidoDto.NotificacionResponse;
import com.ecogestion.dto.UsuarioDto.CambioContrasenaRequest;
import com.ecogestion.dto.UsuarioDto.PerfilRequest;
import com.ecogestion.dto.UsuarioDto.UsuarioResponse;
import com.ecogestion.security.CuentaUsuario;
import com.ecogestion.service.NotificacionService;
import com.ecogestion.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de "Mi perfil" del ciudadano (HU-02) y de su historial de notificaciones (HU-11),
 * rutas bajo {@code /api/ciudadanos/yo}.
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Controlador:</b> es el primer objeto que recibe la operación del sistema que llega desde la
 *       interfaz (una petición HTTP). Solo traduce la petición, delega en el servicio y devuelve la
 *       respuesta; no tiene reglas de negocio.</li>
 *   <li><b>Bajo acoplamiento:</b> recibe y devuelve DTOs (records), así los cambios internos del modelo
 *       no rompen la API que usa el frontend.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/ciudadanos/yo")
public class CiudadanoController {

    private final UsuarioService usuarioService;
    private final NotificacionService notificacionService;

    public CiudadanoController(UsuarioService usuarioService, NotificacionService notificacionService) {
        this.usuarioService = usuarioService;
        this.notificacionService = notificacionService;
    }

    @GetMapping
    public UsuarioResponse perfil(@AuthenticationPrincipal CuentaUsuario cuenta) {
        return UsuarioResponse.de(usuarioService.validarCiudadano(cuenta.getId()));
    }

    @PutMapping
    public UsuarioResponse actualizar(@AuthenticationPrincipal CuentaUsuario cuenta, @Valid @RequestBody PerfilRequest datos) {
        return UsuarioResponse.de(usuarioService.actualizarPerfil(cuenta.getId(), datos));
    }

    @PutMapping("/contrasena")
    public ResponseEntity<Void> cambiarContrasena(@AuthenticationPrincipal CuentaUsuario cuenta,
                                                  @Valid @RequestBody CambioContrasenaRequest datos) {
        usuarioService.cambiarContrasena(cuenta.getUsername(), datos.actual(), datos.nueva());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/notificaciones")
    public List<NotificacionResponse> notificaciones(@AuthenticationPrincipal CuentaUsuario cuenta) {
        return notificacionService.historial(cuenta.getId());
    }
}
