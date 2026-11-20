package com.ecogestion.controller;

import com.ecogestion.dto.UsuarioDto.DisponibilidadRequest;
import com.ecogestion.dto.UsuarioDto.OperadorRequest;
import com.ecogestion.dto.UsuarioDto.OperadorResponse;
import com.ecogestion.dto.UsuarioDto.UsuarioResponse;
import com.ecogestion.model.Operador;
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

/** El administrador crea operadores y consulta los disponibles para asignar (HU-01 nota, HU-07). */
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

    /** {@code disponibles=true}: solo los que se pueden asignar, con sus reportes sin resolver. */
    @GetMapping
    public List<?> listar(@RequestParam(defaultValue = "true") boolean disponibles) {
        if (disponibles) {
            return reporteService.operadoresParaAsignar();
        }
        return usuarioService.consultarOperadores(false).stream()
                .map(o -> new OperadorResponse(o.getId(), o.getNombre(), o.getCorreo(), o.isDisponible(), 0)).toList();
    }

    @PutMapping("/{id}/disponibilidad")
    public UsuarioResponse cambiarDisponibilidad(@PathVariable String id, @RequestBody DisponibilidadRequest datos) {
        Operador operador = usuarioService.cambiarDisponibilidad(id, datos.disponible());
        return UsuarioResponse.de(operador);
    }
}
