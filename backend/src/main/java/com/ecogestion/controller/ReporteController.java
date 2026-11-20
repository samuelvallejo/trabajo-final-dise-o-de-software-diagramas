package com.ecogestion.controller;

import com.ecogestion.dto.ReporteDto.AsignacionRequest;
import com.ecogestion.dto.ReporteDto.ClasificacionRequest;
import com.ecogestion.dto.ReporteDto.DuplicadoRequest;
import com.ecogestion.dto.ReporteDto.PosibleDuplicadoResponse;
import com.ecogestion.dto.ReporteDto.ReporteRequest;
import com.ecogestion.dto.ReporteDto.ReporteResponse;
import com.ecogestion.model.EstadoReporte;
import com.ecogestion.model.Prioridad;
import com.ecogestion.model.TipoResiduo;
import com.ecogestion.security.CuentaUsuario;
import com.ecogestion.service.ReporteService;
import com.ecogestion.service.ReporteService.FiltroReportes;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * API de reportes ({@code «RestController» ReporteController} de la vista de desarrollo), rutas bajo {@code /api/reportes}:
 * el ciudadano registra y consulta sus reportes (HU-03, HU-04, HU-10) y el administrador revisa la bandeja,
 * clasifica, gestiona duplicados y asigna operadores (HU-05, HU-06, HU-07).
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
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    /** El ciudadano crea un reporte con descripción, tipo, ubicación y de 1 a 5 fotos (HU-03, HU-04). */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReporteResponse> registrarReporte(@AuthenticationPrincipal CuentaUsuario cuenta,
                                                            @Valid @RequestPart("datos") ReporteRequest request,
                                                            @RequestPart(value = "fotos", required = false) List<MultipartFile> fotos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reporteService.registrarReporte(request, fotos, cuenta.getId()));
    }

    /** Ciudadano: su historial (HU-10). Administrador: bandeja de revisión con filtros (HU-05). */
    @GetMapping
    public ResponseEntity<List<ReporteResponse>> listarReportes(@AuthenticationPrincipal CuentaUsuario cuenta,
                                                @RequestParam(required = false) EstadoReporte estado,
                                                @RequestParam(required = false) TipoResiduo tipoResiduo,
                                                @RequestParam(required = false) Prioridad prioridad,
                                                @RequestParam(required = false) String sector) {
        return ResponseEntity.ok(reporteService.listarReportes(cuenta.getId(), cuenta.getRol(),
                new FiltroReportes(estado, tipoResiduo, prioridad, sector)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReporteResponse> obtenerReporte(@AuthenticationPrincipal CuentaUsuario cuenta, @PathVariable String id) {
        return ResponseEntity.ok(reporteService.obtenerReporte(id, cuenta.getId(), cuenta.getRol()));
    }

    /** Asigna o reasigna el reporte a un operador disponible (HU-07). */
    @PutMapping("/{id}/asignacion")
    public ResponseEntity<ReporteResponse> asignarReporte(@PathVariable String id, @Valid @RequestBody AsignacionRequest request) {
        return ResponseEntity.ok(reporteService.asignarReporte(id, request.operadorId(), request.reasignar()));
    }

    /** Cambia el tipo de problema y la prioridad (HU-05). */
    @PutMapping("/{id}/clasificacion")
    public ReporteResponse clasificar(@PathVariable String id, @Valid @RequestBody ClasificacionRequest request) {
        return reporteService.clasificarReporte(id, request.tipoResiduo(), request.prioridad());
    }

    /** Reportes abiertos del mismo tipo a menos de 50 m (HU-06, criterio 1). */
    @GetMapping("/{id}/posibles-duplicados")
    public List<PosibleDuplicadoResponse> posiblesDuplicados(@PathVariable String id) {
        return reporteService.posiblesDuplicados(id);
    }

    @PutMapping("/{id}/duplicado")
    public ReporteResponse marcarDuplicado(@PathVariable String id, @Valid @RequestBody DuplicadoRequest request) {
        return reporteService.marcarComoDuplicado(id, request.reporteOriginalId());
    }

    @DeleteMapping("/{id}/duplicado")
    public ReporteResponse desvincularDuplicado(@PathVariable String id) {
        return reporteService.desvincularDuplicado(id);
    }
}
