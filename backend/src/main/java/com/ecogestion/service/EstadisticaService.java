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
 * Lógica del componente "Estadísticas e informes" (HU-15): panel del administrador, informe por periodo
 * y sector, y exportación a CSV. Los reportes duplicados no se cuentan dos veces.
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Fabricación pura:</b> calcular indicadores no es responsabilidad de ningún objeto del dominio;
 *       se crea esta clase para no cargar al {@link Reporte} con lógica de informes.</li>
 *   <li><b>Bajo acoplamiento:</b> no accede a MongoDB; obtiene los datos con los servicios "Consultar
 *       reportes" ({@link ReporteService}) y "Consultar tipos y prioridades" ({@link ClasificacionService}),
 *       tal como muestra la vista funcional.</li>
 *   <li><b>Experto en información:</b> el tiempo de atención de cada reporte lo calcula el propio reporte
 *       ({@link Reporte#horasDeAtencion()}).</li>
 * </ul>
 */
@Service
public class EstadisticaService {

    private final ReporteService reporteService;
    private final ClasificacionService clasificacionService;

    public EstadisticaService(ReporteService reporteService, ClasificacionService clasificacionService) {
        this.reporteService = reporteService;
        this.clasificacionService = clasificacionService;
    }

    /** Panel: totales por estado, problemas más frecuentes y sectores con más reportes (HU-15, criterios 1 y 2). */
    public PanelResponse panel() {
        List<Reporte> reportes = reportesUnicos();
        return new PanelResponse(reportes.size(),
                contar(reportes, EstadoReporte.PENDIENTE), contar(reportes, EstadoReporte.ASIGNADO),
                contar(reportes, EstadoReporte.EN_PROCESO), contar(reportes, EstadoReporte.RESUELTO),
                problemasFrecuentes(reportes), porSector(reportes));
    }

    /** Informe de un periodo (y opcionalmente de un sector) con el tiempo promedio de atención (HU-15, criterio 3). */
    public InformeResponse informe(LocalDate desde, LocalDate hasta, String sector) {
        List<Reporte> reportes = reportesUnicos().stream()
                .filter(r -> estaEnPeriodo(r, desde, hasta))
                .filter(r -> sector == null || sector.isBlank() || sector.equalsIgnoreCase(r.getSector()))
                .toList();

        OptionalDouble promedio = reportes.stream().map(Reporte::horasDeAtencion).filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue).average();
        Double promedioRedondeado = promedio.isPresent() ? Math.round(promedio.getAsDouble() * 10) / 10.0 : null;

        Map<Prioridad, Long> porPrioridad = new EnumMap<>(Prioridad.class);
        for (Prioridad p : Prioridad.values()) {
            porPrioridad.put(p, reportes.stream().filter(r -> r.getPrioridad() == p).count());
        }
        long pendientes = reportes.stream().filter(r -> r.getEstado() != EstadoReporte.RESUELTO).count();

        return new InformeResponse(desde, hasta, sector, reportes.size(), pendientes,
                contar(reportes, EstadoReporte.RESUELTO), promedioRedondeado,
                problemasFrecuentes(reportes), porPrioridad, porSector(reportes));
    }

    /** Convierte el informe en un CSV (separado por punto y coma) para descargarlo (HU-15, criterio 4). */
    public String exportarCsv(InformeResponse informe) {
        StringBuilder csv = new StringBuilder("indicador;valor\n");
        fila(csv, "desde", informe.desde());
        fila(csv, "hasta", informe.hasta());
        fila(csv, "sector", informe.sector() == null ? "Todos" : informe.sector());
        fila(csv, "total reportes", informe.totalReportes());
        fila(csv, "pendientes", informe.pendientes());
        fila(csv, "resueltos", informe.resueltos());
        fila(csv, "tiempo promedio de atención (horas)",
                informe.tiempoPromedioAtencionHoras() == null ? "" : informe.tiempoPromedioAtencionHoras());
        informe.porTipoResiduo().forEach((tipo, n) -> fila(csv, "tipo " + tipo, n));
        informe.porPrioridad().forEach((p, n) -> fila(csv, "prioridad " + p, n));
        informe.porSector().forEach(s -> fila(csv, "sector " + s.sector(), s.reportes()));
        return csv.toString();
    }

    // ============================================================ métodos privados de apoyo

    /** Todos los reportes menos los duplicados (cada problema se cuenta una sola vez). */
    private List<Reporte> reportesUnicos() {
        return reporteService.consultarReportes().stream().filter(r -> !r.esDuplicado()).toList();
    }

    private static boolean estaEnPeriodo(Reporte r, LocalDate desde, LocalDate hasta) {
        LocalDate fecha = r.getFechaReporte().toLocalDate();
        return !fecha.isBefore(desde) && !fecha.isAfter(hasta);
    }

    private static long contar(List<Reporte> reportes, EstadoReporte estado) {
        return reportes.stream().filter(r -> r.getEstado() == estado).count();
    }

    /** Cantidad de reportes por tipo de residuo, de mayor a menor. */
    private Map<TipoResiduo, Long> problemasFrecuentes(List<Reporte> reportes) {
        Map<TipoResiduo, Long> conteo = new EnumMap<>(TipoResiduo.class);
        clasificacionService.tiposYPrioridades().keySet().forEach(t -> conteo.put(t, 0L));
        reportes.forEach(r -> conteo.merge(r.getTipoResiduo(), 1L, Long::sum));
        return conteo.entrySet().stream()
                .sorted(Map.Entry.<TipoResiduo, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    /** Ranking de sectores con más reportes y su punto medio (sirve para dibujar un mapa de calor). */
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

    private static void fila(StringBuilder csv, String indicador, Object valor) {
        csv.append(indicador).append(';').append(valor).append('\n');
    }
}
