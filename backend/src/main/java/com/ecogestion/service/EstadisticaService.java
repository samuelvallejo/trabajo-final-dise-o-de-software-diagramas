package com.ecogestion.service;

import com.ecogestion.dto.EstadisticaDto.ConteoSector;
import com.ecogestion.dto.EstadisticaDto.InformeResponse;
import com.ecogestion.dto.EstadisticaDto.PanelResponse;
import com.ecogestion.model.EstadisticasReportes;
import com.ecogestion.model.EstadoReporte;
import com.ecogestion.model.TipoResiduo;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Lógica del componente "Estadísticas e informes" (HU-15): panel del administrador e informe por periodo
 * y sector. Los cálculos los hace el objeto {@link EstadisticasReportes}; este servicio obtiene los datos
 * y arma las respuestas.
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Bajo acoplamiento:</b> no accede a MongoDB; usa los servicios "Consultar reportes"
 *       ({@link ReporteService}) y "Consultar tipos y prioridades" ({@link ClasificacionService}), tal como
 *       muestra la vista funcional.</li>
 *   <li><b>Experto en información:</b> los indicadores los calcula {@link EstadisticasReportes}, que es
 *       quien tiene los reportes; el CSV lo genera el propio informe ({@link InformeResponse#aCsv()}).</li>
 *   <li><b>Fabricación pura:</b> coordina la consulta y la conversión a DTO.</li>
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
        EstadisticasReportes estadisticas = todas();
        return new PanelResponse(estadisticas.total(),
                estadisticas.conEstado(EstadoReporte.PENDIENTE), estadisticas.conEstado(EstadoReporte.ASIGNADO),
                estadisticas.conEstado(EstadoReporte.EN_PROCESO), estadisticas.conEstado(EstadoReporte.RESUELTO),
                estadisticas.porTipoResiduo(tipos()), sectores(estadisticas));
    }

    /** Informe de un periodo (y opcionalmente de un sector) con el tiempo promedio de atención (HU-15, criterio 3). */
    public InformeResponse informe(LocalDate desde, LocalDate hasta, String sector) {
        EstadisticasReportes estadisticas = todas().delPeriodo(desde, hasta).delSector(sector);
        return new InformeResponse(desde, hasta, sector, estadisticas.total(), estadisticas.sinResolver(),
                estadisticas.conEstado(EstadoReporte.RESUELTO), estadisticas.horasPromedioDeAtencion(),
                estadisticas.porTipoResiduo(tipos()), estadisticas.porPrioridad(), sectores(estadisticas));
    }

    /** Todos los reportes sin duplicados, con el servicio "Consultar reportes". */
    private EstadisticasReportes todas() {
        return EstadisticasReportes.de(reporteService.consultarReportes());
    }

    /** Servicio "Consultar tipos y prioridades" de Clasificación y priorización (vista funcional). */
    private Set<TipoResiduo> tipos() {
        return clasificacionService.tiposYPrioridades().keySet();
    }

    private static List<ConteoSector> sectores(EstadisticasReportes estadisticas) {
        return estadisticas.porSector().stream()
                .map(a -> new ConteoSector(a.sector(), a.reportes(), a.centro().latitud(), a.centro().longitud()))
                .toList();
    }
}
