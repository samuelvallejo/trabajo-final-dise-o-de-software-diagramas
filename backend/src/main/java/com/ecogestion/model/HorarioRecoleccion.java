package com.ecogestion.model;

import com.ecogestion.excepcion.DatosInvalidosException;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Franja de recolección de una ruta (objeto de valor embebido en {@link RutaRecoleccion}).
 * La hora de fin debe ser posterior a la de inicio (HU-12, criterio 4).
 */
public record HorarioRecoleccion(DayOfWeek diaSemana, LocalTime horaInicio, LocalTime horaFin) {

    public HorarioRecoleccion {
        Objects.requireNonNull(diaSemana, "El día es obligatorio");
        Objects.requireNonNull(horaInicio, "La hora de inicio es obligatoria");
        Objects.requireNonNull(horaFin, "La hora de fin es obligatoria");
        if (!horaFin.isAfter(horaInicio)) {
            throw new DatosInvalidosException("La hora de fin debe ser posterior a la hora de inicio");
        }
    }
}
