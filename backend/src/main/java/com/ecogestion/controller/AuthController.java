package com.ecogestion.controller;

import com.ecogestion.dto.UsuarioDto.LoginRequest;
import com.ecogestion.dto.UsuarioDto.RegistroRequest;
import com.ecogestion.dto.UsuarioDto.UsuarioResponse;
import com.ecogestion.security.CuentaUsuario;
import com.ecogestion.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API de registro e inicio de sesión (HU-01), rutas bajo {@code /api/auth}. El inicio de sesión crea una sesión
 * HTTP: el navegador recibe una cookie y la envía en las siguientes peticiones. El cierre de sesión
 * ({@code POST /api/auth/logout}) lo atiende Spring Security (ver {@code SeguridadConfig}).
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
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService usuarioService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository repositorioContexto;

    public AuthController(UsuarioService usuarioService, AuthenticationManager authenticationManager,
                          SecurityContextRepository repositorioContexto) {
        this.usuarioService = usuarioService;
        this.authenticationManager = authenticationManager;
        this.repositorioContexto = repositorioContexto;
    }

    /** Crea la cuenta con rol Ciudadano (HU-01, criterios 1 a 3). */
    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroRequest datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.de(usuarioService.registrarCiudadano(datos)));
    }

    /** Inicia sesión y guarda la sesión HTTP (HU-01, criterios 4 y 5). */
    @PostMapping("/login")
    public UsuarioResponse iniciarSesion(@Valid @RequestBody LoginRequest datos, HttpServletRequest request,
                                         HttpServletResponse response) {
        Authentication autenticacion = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(datos.correo().trim().toLowerCase(), datos.contrasena()));
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacion);
        SecurityContextHolder.setContext(contexto);
        repositorioContexto.saveContext(contexto, request, response);
        return yo((CuentaUsuario) autenticacion.getPrincipal());
    }

    @GetMapping("/yo")
    public UsuarioResponse yo(@AuthenticationPrincipal CuentaUsuario cuenta) {
        return usuarioService.buscarPorCorreo(cuenta.getUsername()).map(UsuarioResponse::de).orElseThrow();
    }
}
