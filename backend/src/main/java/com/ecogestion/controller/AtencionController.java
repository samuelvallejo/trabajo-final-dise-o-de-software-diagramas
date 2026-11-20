package com.ecogestion.controller;

import com.ecogestion.dto.ReporteDto.ReporteResponse;
import com.ecogestion.security.CuentaUsuario;
import com.ecogestion.service.AtencionService;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** El operador de limpieza consulta y atiende sus reportes asignados (HU-08, HU-09). */
@RestController
@RequestMapping("/api/operador/asignaciones")
public class AtencionController {

    private final AtencionService atencionService;

    public AtencionController(AtencionService atencionService) {
        this.atencionService = atencionService;
    }

    /** {@code orden=prioridad} (por defecto) o {@code orden=fecha}. */
    @GetMapping
    public List<ReporteResponse> misAsignaciones(@AuthenticationPrincipal CuentaUsuario cuenta,
                                                 @RequestParam(defaultValue = "prioridad") String orden) {
        return atencionService.misAsignaciones(cuenta.getId(), orden);
    }

    @GetMapping("/{id}")
    public ReporteResponse detalle(@AuthenticationPrincipal CuentaUsuario cuenta, @PathVariable String id) {
        return atencionService.detalle(id, cuenta.getId());
    }

    @PostMapping("/{id}/inicio")
    public ReporteResponse iniciarAtencion(@AuthenticationPrincipal CuentaUsuario cuenta, @PathVariable String id) {
        return atencionService.iniciarAtencion(id, cuenta.getId());
    }

    @PostMapping(value = "/{id}/cierre", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ReporteResponse registrarCierre(@AuthenticationPrincipal CuentaUsuario cuenta, @PathVariable String id,
                                           @RequestParam("descripcionTrabajo") String descripcionTrabajo,
                                           @RequestPart(value = "evidencias", required = false) List<MultipartFile> evidencias) {
        return atencionService.registrarCierre(id, cuenta.getId(), descripcionTrabajo, evidencias);
    }
}
