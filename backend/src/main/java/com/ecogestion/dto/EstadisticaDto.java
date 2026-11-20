package com.ecogestion.dto;

import com.ecogestion.model.Prioridad;
import com.ecogestion.model.TipoResiduo;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** DTO del módulo Estadísticas e informes (HU-15). */
public final class EstadisticaDto {

    private EstadisticaDto() {
    }

    /** Cantidad de reportes de un sector con su punto medio, para el ranking y el mapa de calor. */
    public record ConteoSector(String sector, long reportes, double latitudPromedio, double longitudPromedio) { }

    public record PanelResponse(
            long totalReportes,
            long pendientes,
            long asignados,
            long enProceso,
            long resueltos,
            Map<TipoResiduo, Long> problemasFrecuentes,
            List<ConteoSector> sectoresMayorAcumulacion) { }

    public record InformeResponse(
            LocalDate desde,
            LocalDate hasta,
            String sector,
            long totalReportes,
            long pendientes,
            long resueltos,
            Double tiempoPromedioAtencionHoras,
            Map<TipoResiduo, Long> porTipoResiduo,
            Map<Prioridad, Long> porPrioridad,
            List<ConteoSector> porSector) { }
}
