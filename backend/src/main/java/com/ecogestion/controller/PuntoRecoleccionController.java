package com.ecogestion.controller;

import com.ecogestion.dto.RecoleccionDto.PuntoRequest;
import com.ecogestion.dto.RecoleccionDto.PuntoResponse;
import com.ecogestion.model.MaterialAceptado;
import com.ecogestion.security.CuentaUsuario;
import com.ecogestion.service.PuntoRecoleccionService;
import com.ecogestion.service.UsuarioService;
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

/** Puntos de recolección autorizados en el mapa (HU-13). */
@RestController
public class PuntoRecoleccionController {

    private final PuntoRecoleccionService puntoService;
    private final UsuarioService usuarioService;

    public PuntoRecoleccionController(PuntoRecoleccionService puntoService, UsuarioService usuarioService) {
        this.puntoService = puntoService;
        this.usuarioService = usuarioService;
    }

    /** Filtros opcionales por material y sector; con latitud/longitud se ordena por cercanía. */
    @GetMapping("/api/puntos")
    public List<PuntoResponse> listar(@RequestParam(required = false) MaterialAceptado material,
                                      @RequestParam(required = false) String sector,
                                      @RequestParam(required = false) Double latitud,
                                      @RequestParam(required = false) Double longitud) {
        return puntoService.listar(material, sector, latitud, longitud);
    }

    /** Puntos del sector registrado del ciudadano (HU-13, criterio 4). */
    @GetMapping("/api/puntos/mi-sector")
    public List<PuntoResponse> deMiSector(@AuthenticationPrincipal CuentaUsuario cuenta,
                                          @RequestParam(required = false) MaterialAceptado material) {
        return puntoService.listar(material, usuarioService.validarCiudadano(cuenta.getId()).getSector(), null, null);
    }

    @GetMapping("/api/admin/puntos")
    public List<PuntoResponse> todos() {
        return puntoService.listarTodos();
    }

    @PostMapping("/api/admin/puntos")
    public ResponseEntity<PuntoResponse> crear(@Valid @RequestBody PuntoRequest datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(puntoService.crear(datos));
    }

    @PutMapping("/api/admin/puntos/{id}")
    public PuntoResponse actualizar(@PathVariable String id, @Valid @RequestBody PuntoRequest datos) {
        return puntoService.actualizar(id, datos);
    }

    @DeleteMapping("/api/admin/puntos/{id}")
    public PuntoResponse desactivar(@PathVariable String id) {
        return puntoService.cambiarActivacion(id, false);
    }

    @PostMapping("/api/admin/puntos/{id}/activacion")
    public PuntoResponse activar(@PathVariable String id) {
        return puntoService.cambiarActivacion(id, true);
    }
}
