package com.ecogestion.dto;

import com.ecogestion.model.EstadoReporte;
import com.ecogestion.model.Prioridad;
import com.ecogestion.model.Reporte;
import com.ecogestion.model.TipoResiduo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Objetos de transferencia (DTO) de reportes: crear, clasificar, asignar y marcar duplicados (HU-03 a HU-10).
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
public final class ReporteDto {

    private ReporteDto() {
    }

    /** Datos del reporte (las fotos llegan como archivos en la misma solicitud multipart). */
    public record ReporteRequest(
            @NotBlank String descripcion,
            @NotNull TipoResiduo tipoResiduo,
            @NotNull Double latitud,
            @NotNull Double longitud) { }

    public record AsignacionRequest(@NotBlank String operadorId, boolean reasignar) { }

    public record ClasificacionRequest(@NotNull TipoResiduo tipoResiduo, @NotNull Prioridad prioridad) { }

    public record DuplicadoRequest(@NotBlank String reporteOriginalId) { }

    public record PersonaResumen(String id, String nombre) { }

    public record ReporteResponse(
            String id,
            String descripcion,
            TipoResiduo tipoResiduo,
            Prioridad prioridad,
            double latitud,
            double longitud,
            String direccion,
            String sector,
            List<String> fotos,
            LocalDateTime fechaReporte,
            LocalDateTime fechaResolucion,
            EstadoReporte estado,
            String descripcionTrabajo,
            List<String> fotosEvidencia,
            PersonaResumen ciudadano,
            PersonaResumen operador,
            String reporteOriginalId) {

        public static ReporteResponse de(Reporte r) {
            return new ReporteResponse(r.getId(), r.getDescripcion(), r.getTipoResiduo(), r.getPrioridad(),
                    r.getLatitud(), r.getLongitud(), r.getDireccion(), r.getSector(), r.getFotos(),
                    r.getFechaReporte(), r.getFechaResolucion(), r.estadoVisible(), r.getDescripcionTrabajo(),
                    r.getFotosEvidencia(),
                    r.getCiudadano() == null ? null : new PersonaResumen(r.getCiudadano().getId(), r.getCiudadano().getNombre()),
                    r.getOperador() == null ? null : new PersonaResumen(r.getOperador().getId(), r.getOperador().getNombre()),
                    r.esDuplicado() ? r.getReporteOriginal().getId() : null);
        }
    }

    /** Posible duplicado con su distancia al reporte revisado (HU-06). */
    public record PosibleDuplicadoResponse(ReporteResponse reporte, double distanciaMetros) { }
}
