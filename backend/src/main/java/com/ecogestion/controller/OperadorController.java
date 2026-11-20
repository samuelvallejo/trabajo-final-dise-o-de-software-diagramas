package com.ecogestion.controller;

import com.ecogestion.dto.UsuarioDto.DisponibilidadRequest;
import com.ecogestion.dto.UsuarioDto.OperadorRequest;
import com.ecogestion.dto.UsuarioDto.OperadorResponse;
import com.ecogestion.dto.UsuarioDto.UsuarioResponse;
import com.ecogestion.service.ReporteService;
import com.ecogestion.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API con la que el administrador gestiona los operadores de limpieza: crearlos, listarlos con su carga de
 * trabajo y marcarlos como disponibles u ocupados (HU-07). Rutas bajo {@code /api/admin/operadores}.
 *
 * <p><b>Patrón GRASP — Controlador:</b> recibe la petición HTTP, la convierte en una llamada a los
 * servicios y devuelve la respuesta. No contiene reglas de negocio.
 */
@RestController
@RequestMapping("/api/admin/operadores")
public class OperadorController {

    private final UsuarioService usuarioService;
    private final ReporteService reporteService;

    public OperadorController(UsuarioService usuarioService, ReporteService reporteService) {
        this.usuarioService = usuarioService;
        this.reporteService = reporteService;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody OperadorRequest datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.de(usuarioService.crearOperador(datos)));
    }

    /**
     * Operadores con su número de reportes sin resolver, para elegir a quién asignar (HU-07, criterio 4).
     * {@code disponibles=true} (por defecto) muestra solo los que se pueden asignar.
     */
    @GetMapping
    public List<OperadorResponse> listar(@RequestParam(defaultValue = "true") boolean disponibles) {
        return usuarioService.consultarOperadores(disponibles).stream()
                .map(o -> OperadorResponse.de(o, reporteService.contarSinResolver(o)))
                .toList();
    }

    @PutMapping("/{id}/disponibilidad")
    public UsuarioResponse cambiarDisponibilidad(@PathVariable String id, @RequestBody DisponibilidadRequest datos) {
        return UsuarioResponse.de(usuarioService.cambiarDisponibilidad(id, datos.disponible()));
    }
}
