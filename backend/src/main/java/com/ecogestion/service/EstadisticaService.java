package com.ecogestion.service;

import com.ecogestion.dto.EstadisticaDto.ConteoSector;
import com.ecogestion.dto.EstadisticaDto.InformeResponse;
import com.ecogestion.dto.EstadisticaDto.PanelResponse;
import com.ecogestion.model.EstadoReporte;
import com.ecogestion.model.Prioridad;
import com.ecogestion.model.Reporte;
import com.ecogestion.model.TipoResiduo;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

/**
 * Componente "Estadísticas e informes". Requiere "Consultar reportes" (Gestión de reportes) y
 * "Consultar tipos y prioridades" (Clasificación y priorización). Historia HU-15.
 * Los reportes duplicados no se cuentan dos veces.
 */
@Service
public class EstadisticaService {

    private final ReporteService reporteService;
    private final ClasificacionService clasificacionService;

    public EstadisticaService(ReporteService reporteService, ClasificacionService clasificacionService) {
        this.reporteService = reporteService;
        this.clasificacionService = clasificacionService;
    }

    /** Panel administrativo (HU-15, criterios 1 y 2). */
    public PanelResponse panel() {
        List<Reporte> reportes = reportesUnicos();
        return new PanelResponse(reportes.size(),
                contar(reportes, EstadoReporte.PENDIENTE), contar(reportes, EstadoReporte.ASIGNADO),
                contar(reportes, EstadoReporte.EN_PROCESO), contar(reportes, EstadoReporte.RESUELTO),
                problemasFrecuentes(reportes), porSector(reportes));
    }

    /** Informe por periodo y sector opcional, con tiempo promedio de atención (HU-15, criterio 3). */
    public InformeResponse informe(LocalDate desde, LocalDate hasta, String sector) {
        List<Reporte> reportes = reportesUnicos().stream()
                .filter(r -> !r.getFechaReporte().toLocalDate().isBefore(desde) && !r.getFechaReporte().toLocalDate().isAfter(hasta))
                .filter(r -> sector == null || sector.isBlank() || sector.equalsIgnoreCase(r.getSector()))
                .toList();
        OptionalDouble promedio = reportes.stream().map(Reporte::horasDeAtencion).filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue).average();
        Map<Prioridad, Long> porPrioridad = new EnumMap<>(Prioridad.class);
        for (Prioridad p : Prioridad.values()) {
            porPrioridad.put(p, reportes.stream().filter(r -> r.getPrioridad() == p).count());
        }
        return new InformeResponse(desde, hasta, sector, reportes.size(),
                reportes.stream().filter(r -> r.getEstado() != EstadoReporte.RESUELTO).count(),
                contar(reportes, EstadoReporte.RESUELTO),
                promedio.isPresent() ? Math.round(promedio.getAsDouble() * 10) / 10.0 : null,
                problemasFrecuentes(reportes), porPrioridad, porSector(reportes));
    }

    /** Exportación del informe en CSV (HU-15, criterio 4). */
    public String exportarCsv(InformeResponse informe) {
        StringBuilder csv = new StringBuilder("indicador;valor\n")
                .append("desde;").append(informe.desde()).append('\n')
                .append("hasta;").append(informe.hasta()).append('\n')
                .append("sector;").append(informe.sector() == null ? "Todos" : informe.sector()).append('\n')
                .append("total reportes;").append(informe.totalReportes()).append('\n')
                .append("pendientes;").append(informe.pendientes()).append('\n')
                .append("resueltos;").append(informe.resueltos()).append('\n')
                .append("tiempo promedio de atención (horas);")
                .append(informe.tiempoPromedioAtencionHoras() == null ? "" : informe.tiempoPromedioAtencionHoras()).append('\n');
        informe.porTipoResiduo().forEach((tipo, n) -> csv.append("tipo ").append(tipo).append(';').append(n).append('\n'));
        informe.porPrioridad().forEach((p, n) -> csv.append("prioridad ").append(p).append(';').append(n).append('\n'));
        informe.porSector().forEach(s -> csv.append("sector ").append(s.sector()).append(';').append(s.reportes()).append('\n'));
        return csv.toString();
    }

    private List<Reporte> reportesUnicos() {
        return reporteService.consultarReportes().stream().filter(r -> !r.esDuplicado()).toList();
    }

    private static long contar(List<Reporte> reportes, EstadoReporte estado) {
        return reportes.stream().filter(r -> r.getEstado() == estado).count();
    }

    /** Problemas más frecuentes por tipo de residuo, de mayor a menor. */
    private Map<TipoResiduo, Long> problemasFrecuentes(List<Reporte> reportes) {
        Map<TipoResiduo, Long> conteo = new EnumMap<>(TipoResiduo.class);
        clasificacionService.tiposYPrioridades().keySet().forEach(t -> conteo.put(t, 0L));
        reportes.forEach(r -> conteo.merge(r.getTipoResiduo(), 1L, Long::sum));
        return conteo.entrySet().stream()
                .sorted(Map.Entry.<TipoResiduo, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    /** Ranking de sectores con mayor acumulación y su punto medio para el mapa de calor. */
    private static List<ConteoSector> porSector(List<Reporte> reportes) {
        return reportes.stream()
                .collect(Collectors.groupingBy(r -> r.getSector() == null ? "Sin sector" : r.getSector()))
                .entrySet().stream()
                .map(e -> new ConteoSector(e.getKey(), e.getValue().size(),
                        e.getValue().stream().mapToDouble(Reporte::getLatitud).average().orElse(0),
                        e.getValue().stream().mapToDouble(Reporte::getLongitud).average().orElse(0)))
                .sorted(Comparator.comparingLong(ConteoSector::reportes).reversed())
                .toList();
    }
}
