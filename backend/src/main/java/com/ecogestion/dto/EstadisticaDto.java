package com.ecogestion.dto;

import com.ecogestion.model.Prioridad;
import com.ecogestion.model.TipoResiduo;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Objetos de transferencia (DTO) del panel y los informes de estadísticas (HU-15).
 *
 * <p><b>Cómo funciona:</b> cada {@code record} es un objeto inmutable con los datos que entran
 * ({@code ...Request}, validados con anotaciones como {@code @NotBlank}) o que salen ({@code ...Response})
 * de la API en formato JSON. Los métodos {@code de(...)} convierten un objeto del modelo en su respuesta.
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Bajo acoplamiento:</b> la API no expone las clases del modelo; si cambia un documento de
 *       MongoDB, el JSON que usa el frontend no cambia. Tampoco se envían datos sensibles
 *       (por ejemplo el hash de la contraseña).</li>
 *   <li><b>Creador:</b> cada respuesta sabe construirse a partir de su objeto del modelo ({@code de}).</li>
 * </ul>
 */
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
