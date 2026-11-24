package com.ecogestion.model;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

/**
 * Conjunto de reportes sobre el que se calculan los indicadores del panel y de los informes (HU-15).
 * Es una "colección de primera clase": en vez de pasar una {@code List<Reporte>} suelta y calcular con
 * funciones estáticas, la lista vive dentro de un objeto que sabe filtrarse, contarse y agruparse.
 * Es inmutable: cada filtro devuelve un conjunto nuevo.
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Experto en información:</b> el conjunto tiene los reportes, por eso es quien calcula totales,
 *       problemas frecuentes, sectores con más acumulación y tiempo promedio de atención.</li>
 *   <li><b>Alta cohesión:</b> todos los cálculos de indicadores están en una sola clase y se pueden
 *       probar sin base de datos.</li>
 * </ul>
 */
public final class EstadisticasReportes {

    private final List<Reporte> reportes;

    private EstadisticasReportes(List<Reporte> reportes) {
        this.reportes = List.copyOf(reportes);
    }

    /** Crea el conjunto sin los reportes duplicados: cada problema se cuenta una sola vez. */
    public static EstadisticasReportes de(Collection<Reporte> todos) {
        return new EstadisticasReportes(todos.stream().filter(r -> !r.esDuplicado()).toList());
    }

    /** Solo los reportes creados entre las dos fechas (inclusive). */
    public EstadisticasReportes delPeriodo(LocalDate desde, LocalDate hasta) {
        return new EstadisticasReportes(reportes.stream().filter(r -> {
            LocalDate fecha = r.getFechaReporte().toLocalDate();
            return !fecha.isBefore(desde) && !fecha.isAfter(hasta);
        }).toList());
    }

    /** Solo los reportes de un sector; sin sector devuelve el mismo conjunto. */
    public EstadisticasReportes delSector(String sector) {
        if (sector == null || sector.isBlank()) {
            return this;
        }
        return new EstadisticasReportes(reportes.stream().filter(r -> sector.equalsIgnoreCase(r.getSector())).toList());
    }

    public long total() {
        return reportes.size();
    }

    public long conEstado(EstadoReporte estado) {
        return reportes.stream().filter(r -> r.getEstado() == estado).count();
    }

    /** Reportes que todavía no están resueltos. */
    public long sinResolver() {
        return total() - conEstado(EstadoReporte.RESUELTO);
    }

    /** Cantidad por tipo de residuo del catálogo, de mayor a menor (incluye los tipos sin reportes). */
    public Map<TipoResiduo, Long> porTipoResiduo(Collection<TipoResiduo> catalogo) {
        Map<TipoResiduo, Long> conteo = new EnumMap<>(TipoResiduo.class);
        for (TipoResiduo tipo : catalogo) {
            conteo.put(tipo, reportes.stream().filter(r -> r.getTipoResiduo() == tipo).count());
        }
        return conteo.entrySet().stream()
                .sorted(Map.Entry.<TipoResiduo, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
    }

    public Map<Prioridad, Long> porPrioridad() {
        Map<Prioridad, Long> conteo = new EnumMap<>(Prioridad.class);
        for (Prioridad prioridad : Prioridad.values()) {
            conteo.put(prioridad, reportes.stream().filter(r -> r.getPrioridad() == prioridad).count());
        }
        return conteo;
    }

    /** Sectores ordenados de más a menos reportes, con el punto medio de sus reportes (mapa de calor). */
    public List<AcumulacionSector> porSector() {
        return reportes.stream()
                .collect(Collectors.groupingBy(r -> r.getSector() == null ? "Sin sector" : r.getSector()))
                .entrySet().stream()
                .map(e -> new AcumulacionSector(e.getKey(), e.getValue().size(), centro(e.getValue())))
                .sorted(Comparator.comparingLong(AcumulacionSector::reportes).reversed())
                .toList();
    }

    /** Promedio de horas entre la creación y la resolución; null si no hay reportes resueltos. */
    public Double horasPromedioDeAtencion() {
        OptionalDouble promedio = reportes.stream().map(Reporte::horasDeAtencion).filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue).average();
        return promedio.isPresent() ? Math.round(promedio.getAsDouble() * 10) / 10.0 : null;
    }

    private static Coordenada centro(List<Reporte> delSector) {
        return new Coordenada(delSector.stream().mapToDouble(Reporte::getLatitud).average().orElse(0),
                delSector.stream().mapToDouble(Reporte::getLongitud).average().orElse(0));
    }

    /** Cuántos reportes tiene un sector y dónde se concentran. */
    public record AcumulacionSector(String sector, long reportes, Coordenada centro) { }
}
