package com.ecogestion.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/** Pruebas de los objetos que reemplazaron lógica procedimental (switch, funciones estáticas). */
class ObjetosDominioTest {

    private Ciudadano ana;
    private Operador luis;
    private Reporte reporte;

    @BeforeEach
    void preparar() {
        ana = new Ciudadano("Ana", "1", "ana@correo.co", "h", "Centro");
        ReflectionTestUtils.setField(ana, "id", "c1");
        luis = new Operador("Luis", "2", "luis@correo.co", "h");
        ReflectionTestUtils.setField(luis, "id", "o1");
        reporte = new Reporte(LocalDateTime.of(2026, 11, 2, 8, 0), ana, "Basura en la esquina");
        ReflectionTestUtils.setField(reporte, "id", "r1");
        reporte.clasificar(TipoResiduo.DOMESTICO, Prioridad.BAJA);
        reporte.registrarUbicacion(1.2136, -77.2811, "Calle 18", "Centro");
    }

    @Test
    @DisplayName("Polimorfismo: cada rol decide si puede ver un reporte")
    void rolesPolimorficos() {
        reporte.asignar(luis);
        assertThat(Rol.CIUDADANO.puedeVer(reporte, "c1")).isTrue();
        assertThat(Rol.CIUDADANO.puedeVer(reporte, "otro")).isFalse();
        assertThat(Rol.OPERADOR.puedeVer(reporte, "o1")).isTrue();
        assertThat(Rol.OPERADOR.puedeVer(reporte, "o2")).isFalse();
        assertThat(Rol.ADMINISTRADOR.puedeVer(reporte, "cualquiera")).isTrue();
    }

    @Test
    @DisplayName("Polimorfismo: cada rol pide al catálogo la lista que le corresponde")
    void listaPorRol() {
        CatalogoReportes catalogo = new CatalogoReportes() {
            public List<Reporte> historialDelCiudadano(String id) { return List.of(reporte); }
            public List<Reporte> bandejaDeRevision(EstadoReporte estado) { return List.of(); }
            public List<Reporte> pendientesDelOperador(String id) { return List.of(reporte, reporte); }
        };
        assertThat(Rol.CIUDADANO.reportesVisibles(catalogo, "c1", null)).hasSize(1);
        assertThat(Rol.ADMINISTRADOR.reportesVisibles(catalogo, "a1", null)).isEmpty();
        assertThat(Rol.OPERADOR.reportesVisibles(catalogo, "o1", null)).hasSize(2);
    }

    @Test
    @DisplayName("Coordenada: calcula su distancia a otra (unos 111 m por cada milésima de grado de latitud)")
    void distancia() {
        Coordenada plaza = new Coordenada(1.2136, -77.2811);
        assertThat(plaza.distanciaMetros(new Coordenada(1.2146, -77.2811))).isCloseTo(111.2, within(0.5));
        assertThat(plaza.distanciaMetros(plaza)).isZero();
    }

    @Test
    @DisplayName("Estadísticas: no cuentan duplicados y agrupan por tipo y sector")
    void estadisticas() {
        Reporte duplicado = new Reporte(LocalDateTime.of(2026, 11, 3, 9, 0), ana, "La misma basura");
        duplicado.clasificar(TipoResiduo.DOMESTICO, Prioridad.BAJA);
        duplicado.registrarUbicacion(1.2137, -77.2811, "Calle 18", "Centro");
        duplicado.marcarComoDuplicado(reporte);
        Reporte peligroso = new Reporte(LocalDateTime.of(2026, 10, 20, 9, 0), ana, "Pintura");
        peligroso.clasificar(TipoResiduo.PELIGROSO, Prioridad.ALTA);
        peligroso.registrarUbicacion(1.19, -77.27, "Chapal", "Sur");

        EstadisticasReportes todas = EstadisticasReportes.de(List.of(reporte, duplicado, peligroso));
        assertThat(todas.total()).isEqualTo(2);
        assertThat(todas.sinResolver()).isEqualTo(2);
        assertThat(todas.porTipoResiduo(List.of(TipoResiduo.values()))).containsEntry(TipoResiduo.PELIGROSO, 1L);
        assertThat(todas.porSector()).extracting(EstadisticasReportes.AcumulacionSector::sector).containsExactlyInAnyOrder("Centro", "Sur");
        assertThat(todas.delPeriodo(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)).total()).isEqualTo(1);
        assertThat(todas.delSector("Sur").total()).isEqualTo(1);
    }

    @Test
    @DisplayName("Notificación: se redacta sola con el estado y el operador responsable")
    void notificacion() {
        reporte.asignar(luis);
        assertThat(EstadoReporte.ASIGNADO.seNotifica()).isTrue();
        assertThat(EstadoReporte.PENDIENTE.seNotifica()).isFalse();
        Notificacion n = Notificacion.porCambioDeEstado(reporte, reporte, "ana@correo.co");
        assertThat(n.getAsunto()).contains("r1").contains("Asignado");
        assertThat(n.getMensaje()).contains("Hola Ana").contains("Operador responsable: Luis");
    }
}
