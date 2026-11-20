package com.ecogestion.controller;

import com.ecogestion.integracion.imagenes.AlmacenamientoImagenes;
import com.ecogestion.model.Prioridad;
import com.ecogestion.model.TipoResiduo;
import com.ecogestion.service.ClasificacionService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;

/**
 * API de apoyo: catálogo de tipos de residuo con su prioridad sugerida (HU-05) y descarga de las fotos
 * guardadas en GridFS ({@code /api/imagenes/{id}}).
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
public class ApoyoController {

    private final ClasificacionService clasificacionService;
    private final AlmacenamientoImagenes almacenamientoImagenes;

    public ApoyoController(ClasificacionService clasificacionService, AlmacenamientoImagenes almacenamientoImagenes) {
        this.clasificacionService = clasificacionService;
        this.almacenamientoImagenes = almacenamientoImagenes;
    }

    /** Tipos de residuo con su prioridad sugerida (HU-05). */
    @GetMapping("/api/clasificacion/tipos")
    public Map<TipoResiduo, Prioridad> tiposDeResiduo() {
        return clasificacionService.tiposYPrioridades();
    }

    /** Foto de un reporte o evidencia (requiere sesión iniciada). */
    @GetMapping("/api/imagenes/{id}")
    public ResponseEntity<byte[]> imagen(@PathVariable String id) {
        return almacenamientoImagenes.obtener(id)
                .map(img -> ResponseEntity.ok()
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePrivate())
                        .contentType(MediaType.parseMediaType(img.tipoContenido() == null ? "image/jpeg" : img.tipoContenido()))
                        .body(img.contenido()))
                .orElse(ResponseEntity.notFound().build());
    }
}
