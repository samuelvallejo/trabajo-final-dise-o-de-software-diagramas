package com.ecogestion.controller;

import com.ecogestion.dto.RecoleccionDto.HorariosSectorResponse;
import com.ecogestion.dto.RecoleccionDto.RutaRequest;
import com.ecogestion.dto.RecoleccionDto.RutaResponse;
import com.ecogestion.dto.RecoleccionDto.SectorResponse;
import com.ecogestion.excepcion.DatosInvalidosException;
import com.ecogestion.security.CuentaUsuario;
import com.ecogestion.service.HorarioRutaService;
import com.ecogestion.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Horarios y rutas de recolección por sector (HU-12). */
@RestController
public class HorarioRutaController {

    private final HorarioRutaService horarioRutaService;
    private final UsuarioService usuarioService;

    public HorarioRutaController(HorarioRutaService horarioRutaService, UsuarioService usuarioService) {
        this.horarioRutaService = horarioRutaService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/api/sectores")
    public List<SectorResponse> sectores() {
        return horarioRutaService.listarSectores();
    }

    /** Horarios de un sector o barrio buscado por nombre (HU-12, criterio 2). */
    @GetMapping("/api/horarios")
    public HorariosSectorResponse horarios(@RequestParam String sector) {
        return horarioRutaService.horariosDe(sector);
    }

    /** Horarios del sector registrado del ciudadano (HU-12, criterio 1). */
    @GetMapping("/api/horarios/mi-sector")
    public HorariosSectorResponse horariosDeMiSector(@AuthenticationPrincipal CuentaUsuario cuenta) {
        String sector = usuarioService.validarCiudadano(cuenta.getId()).getSector();
        if (sector == null) {
            throw new DatosInvalidosException("Registre su sector en \"Mi perfil\" para ver sus horarios");
        }
        return horarioRutaService.horariosDe(sector);
    }

    @GetMapping("/api/admin/rutas")
    public List<RutaResponse> rutas() {
        return horarioRutaService.listarRutas();
    }

    @PostMapping("/api/admin/rutas")
    public ResponseEntity<RutaResponse> crearRuta(@Valid @RequestBody RutaRequest datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(horarioRutaService.crearRuta(datos));
    }

    @PutMapping("/api/admin/rutas/{id}")
    public RutaResponse actualizarRuta(@PathVariable String id, @Valid @RequestBody RutaRequest datos) {
        return horarioRutaService.actualizarRuta(id, datos);
    }
}
