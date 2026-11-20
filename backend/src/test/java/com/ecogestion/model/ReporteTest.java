package com.ecogestion.model;

import com.ecogestion.excepcion.DatosInvalidosException;
import com.ecogestion.excepcion.ReglaNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReporteTest {

    private Ciudadano ciudadano;
    private Operador operadorA;
    private Operador operadorB;
    private Reporte reporte;

    @BeforeEach
    void preparar() {
        ciudadano = new Ciudadano("Ana", "123", "ana@correo.co", "hash", null);
        ReflectionTestUtils.setField(ciudadano, "id", "c1");
        operadorA = new Operador("Luis", "456", "luis@correo.co", "hash");
        ReflectionTestUtils.setField(operadorA, "id", "o1");
        operadorB = new Operador("Marta", "789", "marta@correo.co", "hash");
        ReflectionTestUtils.setField(operadorB, "id", "o2");
        reporte = nuevoReporte("r1", TipoResiduo.DOMESTICO);
    }

    private Reporte nuevoReporte(String id, TipoResiduo tipo) {
        Reporte r = new Reporte(LocalDateTime.now(), ciudadano, "Basura acumulada en la esquina");
        ReflectionTestUtils.setField(r, "id", id);
        r.clasificar(tipo, tipo.prioridadSugerida());
        r.registrarUbicacion(1.2136, -77.2811, "Centro", "Centro");
        r.adjuntarFotos(List.of("/api/imagenes/1"));
        return r;
    }

    @Test
    @DisplayName("HU-03: la descripción es obligatoria")
    void descripcionObligatoria() {
        assertThatThrownBy(() -> new Reporte(LocalDateTime.now(), ciudadano, " "))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    @DisplayName("HU-03: de 1 a 5 fotos")
    void limiteDeFotos() {
        assertThatThrownBy(() -> reporte.adjuntarFotos(List.of())).isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> reporte.adjuntarFotos(List.of("1", "2", "3", "4", "5", "6")))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    @DisplayName("HU-05: un residuo peligroso siempre tiene prioridad Alta")
    void peligrosoSiempreAlta() {
        assertThat(TipoResiduo.PELIGROSO.prioridadSugerida()).isEqualTo(Prioridad.ALTA);
        assertThatThrownBy(() -> reporte.clasificar(TipoResiduo.PELIGROSO, Prioridad.MEDIA))
                .isInstanceOf(ReglaNegocioException.class);
        reporte.clasificar(TipoResiduo.PELIGROSO, Prioridad.ALTA);
        assertThat(reporte.getPrioridad()).isEqualTo(Prioridad.ALTA);
    }

    @Test
    @DisplayName("HU-07: no se puede asignar un reporte que ya tiene operador")
    void sinDobleAsignacion() {
        reporte.asignar(operadorA);
        assertThat(reporte.getEstado()).isEqualTo(EstadoReporte.ASIGNADO);
        assertThatThrownBy(() -> reporte.asignar(operadorB))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Luis");
    }

    @Test
    @DisplayName("HU-07: reasignar cambia el operador sin dejar dos")
    void reasignar() {
        reporte.asignar(operadorA);
        reporte.reasignar(operadorB);
        assertThat(reporte.getOperador()).isSameAs(operadorB);
        assertThat(reporte.estaAsignadoA("o1")).isFalse();
    }

    @Test
    @DisplayName("HU-09: flujo Pendiente → Asignado → En proceso → Resuelto sin saltos")
    void flujoDeEstados() {
        assertThatThrownBy(reporte::iniciarAtencion).isInstanceOf(ReglaNegocioException.class);
        reporte.asignar(operadorA);
        assertThatThrownBy(() -> reporte.registrarCierre("Limpieza", List.of("/api/imagenes/2")))
                .isInstanceOf(ReglaNegocioException.class);
        reporte.iniciarAtencion();
        reporte.registrarCierre("Se recogieron 3 bolsas", List.of("/api/imagenes/2"));
        assertThat(reporte.getEstado()).isEqualTo(EstadoReporte.RESUELTO);
        assertThat(reporte.getFechaResolucion()).isNotNull();
        assertThat(reporte.horasDeAtencion()).isNotNull();
        assertThatThrownBy(() -> reporte.reasignar(operadorB)).isInstanceOf(ReglaNegocioException.class);
    }

    @Test
    @DisplayName("HU-09: no se puede cerrar sin descripción ni sin evidencias")
    void cierreRequiereDescripcionYEvidencias() {
        reporte.asignar(operadorA);
        reporte.iniciarAtencion();
        assertThatThrownBy(() -> reporte.registrarCierre(" ", List.of("1"))).isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> reporte.registrarCierre("Hecho", List.of())).isInstanceOf(DatosInvalidosException.class);
        assertThat(reporte.getEstado()).isEqualTo(EstadoReporte.EN_PROCESO);
    }

    @Test
    @DisplayName("HU-06: un duplicado refleja el estado del original, no se asigna y al desvincularlo vuelve a Pendiente")
    void duplicados() {
        Reporte original = nuevoReporte("r0", TipoResiduo.DOMESTICO);
        reporte.marcarComoDuplicado(original);
        assertThat(reporte.esDuplicado()).isTrue();
        assertThat(reporte.estaPendiente()).isFalse();
        assertThatThrownBy(() -> reporte.asignar(operadorA)).isInstanceOf(ReglaNegocioException.class);

        original.asignar(operadorA);
        assertThat(reporte.estadoVisible()).isEqualTo(EstadoReporte.ASIGNADO);

        reporte.desvincularDuplicado();
        assertThat(reporte.esDuplicado()).isFalse();
        assertThat(reporte.getEstado()).isEqualTo(EstadoReporte.PENDIENTE);
    }

    @Test
    @DisplayName("HU-06: el original no puede ser a su vez duplicado")
    void originalNoPuedeSerDuplicado() {
        Reporte original = nuevoReporte("r0", TipoResiduo.DOMESTICO);
        Reporte otro = nuevoReporte("r2", TipoResiduo.DOMESTICO);
        original.marcarComoDuplicado(otro);
        assertThatThrownBy(() -> reporte.marcarComoDuplicado(original)).isInstanceOf(ReglaNegocioException.class);
        assertThatThrownBy(() -> reporte.marcarComoDuplicado(reporte)).isInstanceOf(ReglaNegocioException.class);
    }
}
