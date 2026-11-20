package com.ecogestion.model;

import com.ecogestion.excepcion.DatosInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReglasDominioTest {

    @Test
    @DisplayName("HU-01: la cuenta se bloquea 15 minutos tras 5 intentos fallidos")
    void bloqueoPorIntentosFallidos() {
        Ciudadano ciudadano = new Ciudadano("Ana", "123", "Ana@Correo.co", "hash");
        LocalDateTime ahora = LocalDateTime.of(2026, 11, 19, 10, 0);
        for (int i = 0; i < 4; i++) {
            ciudadano.registrarIntentoFallido(ahora, 5, 15);
        }
        assertThat(ciudadano.estaBloqueado(ahora)).isFalse();
        ciudadano.registrarIntentoFallido(ahora, 5, 15);
        assertThat(ciudadano.estaBloqueado(ahora.plusMinutes(14))).isTrue();
        assertThat(ciudadano.estaBloqueado(ahora.plusMinutes(15))).isFalse();
        assertThat(ciudadano.getCorreo()).isEqualTo("ana@correo.co");
    }

    @Test
    @DisplayName("HU-12: la hora de fin debe ser posterior a la de inicio")
    void horarioValido() {
        assertThatThrownBy(() -> new HorarioRecoleccion(DayOfWeek.MONDAY, LocalTime.of(10, 0), LocalTime.of(9, 0)))
                .isInstanceOf(DatosInvalidosException.class);
        assertThat(new HorarioRecoleccion(DayOfWeek.MONDAY, LocalTime.of(6, 0), LocalTime.of(12, 0)).horaFin())
                .isEqualTo(LocalTime.of(12, 0));
    }

    @Test
    @DisplayName("HU-12: una ruta cubre sectores y ordena sus horarios")
    void rutaCubreSectores() {
        RutaRecoleccion ruta = new RutaRecoleccion("Ruta 1", List.of("Centro"),
                List.of(new HorarioRecoleccion(DayOfWeek.FRIDAY, LocalTime.of(19, 0), LocalTime.of(23, 0)),
                        new HorarioRecoleccion(DayOfWeek.MONDAY, LocalTime.of(19, 0), LocalTime.of(23, 0))), null);
        assertThat(ruta.cubreSector("centro")).isTrue();
        assertThat(ruta.getHorarios().get(0).diaSemana()).isEqualTo(DayOfWeek.MONDAY);
    }

    @Test
    @DisplayName("HU-13: un punto informa los materiales que recibe y se puede desactivar")
    void puntoDeRecoleccion() {
        PuntoRecoleccion punto = new PuntoRecoleccion("Ecopunto", "Calle 1", "Centro", 1.21, -77.28,
                EnumSet.of(MaterialAceptado.PILAS), "8:00–17:00");
        assertThat(punto.recibeMaterial(MaterialAceptado.PILAS)).isTrue();
        assertThat(punto.recibeMaterial(MaterialAceptado.ELECTRONICOS)).isFalse();
        punto.desactivar();
        assertThat(punto.isActivo()).isFalse();
    }

    @Test
    @DisplayName("HU-14: un contenido nuevo es borrador hasta publicarlo")
    void contenidoBorrador() {
        ContenidoEducativo contenido = new ContenidoEducativo("Reciclaje", CategoriaContenido.RECICLAJE,
                "Separe el vidrio", null, "a1");
        assertThat(contenido.isPublicado()).isFalse();
        contenido.publicar();
        assertThat(contenido.isPublicado()).isTrue();
        assertThat(contenido.contiene("VIDRIO")).isTrue();
    }

    @Test
    @DisplayName("HU-09: solo se avanza al estado siguiente")
    void transicionesDeEstado() {
        assertThat(EstadoReporte.PENDIENTE.puedeAvanzarA(EstadoReporte.ASIGNADO)).isTrue();
        assertThat(EstadoReporte.ASIGNADO.puedeAvanzarA(EstadoReporte.RESUELTO)).isFalse();
        assertThat(EstadoReporte.EN_PROCESO.puedeAvanzarA(EstadoReporte.ASIGNADO)).isFalse();
    }
}
