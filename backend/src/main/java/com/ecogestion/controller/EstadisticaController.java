package com.ecogestion.controller;

import com.ecogestion.dto.EstadisticaDto.InformeResponse;
import com.ecogestion.dto.EstadisticaDto.PanelResponse;
import com.ecogestion.excepcion.DatosInvalidosException;
import com.ecogestion.service.EstadisticaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/** Panel administrativo, estadísticas e informes (HU-15). */
@RestController
@RequestMapping("/api/admin/estadisticas")
public class EstadisticaController {

    private final EstadisticaService estadisticaService;

    public EstadisticaController(EstadisticaService estadisticaService) {
        this.estadisticaService = estadisticaService;
    }

    @GetMapping("/panel")
    public PanelResponse panel() {
        return estadisticaService.panel();
    }

    @GetMapping("/informe")
    public InformeResponse informe(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                   @RequestParam(required = false) String sector) {
        if (hasta.isBefore(desde)) {
            throw new DatosInvalidosException("La fecha final no puede ser anterior a la inicial");
        }
        return estadisticaService.informe(desde, hasta, sector);
    }

    /** Descarga del informe en CSV (HU-15, criterio 4). */
    @GetMapping("/informe.csv")
    public ResponseEntity<byte[]> informeCsv(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                             @RequestParam(required = false) String sector) {
        String csv = estadisticaService.exportarCsv(informe(desde, hasta, sector));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"informe-ecogestion-" + desde + "-" + hasta + ".csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }
}
