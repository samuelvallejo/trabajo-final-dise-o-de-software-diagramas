package com.ecogestion.dto;

import com.ecogestion.model.HorarioRecoleccion;
import com.ecogestion.model.MaterialAceptado;
import com.ecogestion.model.PuntoRecoleccion;
import com.ecogestion.model.RutaRecoleccion;
import com.ecogestion.model.Sector;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/**
 * Objetos de transferencia (DTO) de Horarios y rutas (HU-12) y Puntos de recolección (HU-13).
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
public final class RecoleccionDto {

    private RecoleccionDto() {
    }

    public record SectorResponse(String id, String nombre, String comuna, List<String> barrios) {
        public static SectorResponse de(Sector s) {
            return new SectorResponse(s.getId(), s.getNombre(), s.getComuna(), s.getBarrios());
        }
    }

    public record HorarioRequest(@NotNull DayOfWeek diaSemana, @NotNull LocalTime horaInicio, @NotNull LocalTime horaFin) {
        public HorarioRecoleccion aDominio() {
            return new HorarioRecoleccion(diaSemana, horaInicio, horaFin);
        }
    }

    public record RutaRequest(
            @NotBlank String nombre,
            @NotEmpty List<String> sectores,
            @NotEmpty List<@Valid HorarioRequest> horarios,
            List<RutaRecoleccion.Coordenada> recorrido) { }

    public record RutaResponse(String id, String nombre, List<String> sectores, List<HorarioRecoleccion> horarios,
                               List<RutaRecoleccion.Coordenada> recorrido) {
        public static RutaResponse de(RutaRecoleccion r) {
            return new RutaResponse(r.getId(), r.getNombre(), r.getSectores(), r.getHorarios(), r.getRecorrido());
        }
    }

    /** Horarios y rutas de un sector (HU-12, criterios 1 y 2). */
    public record HorariosSectorResponse(SectorResponse sector, List<RutaResponse> rutas) { }

    public record PuntoRequest(
            @NotBlank String nombre,
            @NotBlank String direccion,
            String sector,
            @NotNull Double latitud,
            @NotNull Double longitud,
            @NotEmpty Set<MaterialAceptado> materiales,
            @NotBlank String horarioAtencion) { }

    public record PuntoResponse(String id, String nombre, String direccion, String sector, double latitud,
                                double longitud, Set<MaterialAceptado> materiales, String horarioAtencion,
                                boolean activo, Double distanciaMetros) {
        public static PuntoResponse de(PuntoRecoleccion p, Double distanciaMetros) {
            return new PuntoResponse(p.getId(), p.getNombre(), p.getDireccion(), p.getSector(), p.getLatitud(),
                    p.getLongitud(), p.getMateriales(), p.getHorarioAtencion(), p.isActivo(), distanciaMetros);
        }
    }
}
