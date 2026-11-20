package com.ecogestion.controller;

import com.ecogestion.dto.ContenidoDto.ContenidoRequest;
import com.ecogestion.dto.ContenidoDto.ContenidoResponse;
import com.ecogestion.model.CategoriaContenido;
import com.ecogestion.security.CuentaUsuario;
import com.ecogestion.service.ContenidoEducativoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de contenido de educación ambiental (HU-14). Los ciudadanos leen lo publicado
 * ({@code /api/contenidos}); el administrador lo gestiona ({@code /api/admin/contenidos}).
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
public class ContenidoEducativoController {

    private final ContenidoEducativoService contenidoService;

    public ContenidoEducativoController(ContenidoEducativoService contenidoService) {
        this.contenidoService = contenidoService;
    }

    @GetMapping("/api/contenidos")
    public List<ContenidoResponse> publicados(@RequestParam(required = false) CategoriaContenido categoria,
                                              @RequestParam(required = false, name = "q") String palabraClave) {
        return contenidoService.listarPublicados(categoria, palabraClave);
    }

    @GetMapping("/api/contenidos/{id}")
    public ContenidoResponse publicado(@PathVariable String id) {
        return contenidoService.obtenerPublicado(id);
    }

    @GetMapping("/api/admin/contenidos")
    public List<ContenidoResponse> todos(@AuthenticationPrincipal CuentaUsuario cuenta) {
        return contenidoService.listarTodos(cuenta.getRol());
    }

    @PostMapping("/api/admin/contenidos")
    public ResponseEntity<ContenidoResponse> crear(@AuthenticationPrincipal CuentaUsuario cuenta,
                                                   @Valid @RequestBody ContenidoRequest datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contenidoService.crear(datos, cuenta.getId(), cuenta.getRol()));
    }

    @PutMapping("/api/admin/contenidos/{id}")
    public ContenidoResponse actualizar(@AuthenticationPrincipal CuentaUsuario cuenta, @PathVariable String id,
                                        @Valid @RequestBody ContenidoRequest datos) {
        return contenidoService.actualizar(id, datos, cuenta.getRol());
    }

    @PostMapping("/api/admin/contenidos/{id}/publicacion")
    public ContenidoResponse publicar(@AuthenticationPrincipal CuentaUsuario cuenta, @PathVariable String id) {
        return contenidoService.cambiarPublicacion(id, true, cuenta.getRol());
    }

    @DeleteMapping("/api/admin/contenidos/{id}/publicacion")
    public ContenidoResponse despublicar(@AuthenticationPrincipal CuentaUsuario cuenta, @PathVariable String id) {
        return contenidoService.cambiarPublicacion(id, false, cuenta.getRol());
    }
}
