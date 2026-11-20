package com.ecogestion.model;

import com.ecogestion.excepcion.DatosInvalidosException;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Franja de recolección de una ruta: día de la semana y horas de inicio y fin. Es un objeto de valor
 * (un {@code record} inmutable) que se guarda dentro del documento de {@link RutaRecoleccion}.
 *
 * <p><b>Patrón GRASP — Experto en información:</b> el propio horario valida, al construirse, que la hora
 * de fin sea posterior a la de inicio (HU-12, criterio 4); no puede existir un horario inválido.
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
