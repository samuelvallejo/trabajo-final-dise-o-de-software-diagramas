package com.ecogestion.service;

import com.ecogestion.config.EcoGestionProperties;
import com.ecogestion.dto.ReporteDto.ReporteRequest;
import com.ecogestion.dto.ReporteDto.ReporteResponse;
import com.ecogestion.excepcion.DatosInvalidosException;
import com.ecogestion.excepcion.ReglaNegocioException;
import com.ecogestion.integracion.imagenes.AlmacenamientoImagenes;
import com.ecogestion.integracion.mapas.ServicioMapas;
import com.ecogestion.model.Ciudadano;
import com.ecogestion.model.EstadoReporte;
import com.ecogestion.model.Operador;
import com.ecogestion.model.Prioridad;
import com.ecogestion.model.Reporte;
import com.ecogestion.model.TipoResiduo;
import com.ecogestion.repository.CiudadanoRepository;
import com.ecogestion.repository.OperadorRepository;
import com.ecogestion.repository.ReporteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReporteServiceTest {

    private CiudadanoRepository ciudadanoRepository;
    private OperadorRepository operadorRepository;
    private ReporteRepository reporteRepository;
    private HorarioRutaService horarioRutaService;
    private NotificacionService notificacionService;
    private ServicioMapas servicioMapas;
    private AlmacenamientoImagenes almacenamiento;
    private ReporteService servicio;
    private Ciudadano ciudadano;
    private Operador operador;

    @BeforeEach
    void preparar() {
        ciudadanoRepository = mock(CiudadanoRepository.class);
        operadorRepository = mock(OperadorRepository.class);
        reporteRepository = mock(ReporteRepository.class);
        horarioRutaService = mock(HorarioRutaService.class);
        notificacionService = mock(NotificacionService.class);
        servicioMapas = mock(ServicioMapas.class);
        almacenamiento = mock(AlmacenamientoImagenes.class);
        EcoGestionProperties propiedades = new EcoGestionProperties(
                new EcoGestionProperties.Notificaciones("registro", "no-responder@ecogestion.co", 3),
                new EcoGestionProperties.Mapas("coordenadas"), new EcoGestionProperties.Duplicados(50),
                new EcoGestionProperties.Seguridad(5, 15), new EcoGestionProperties.Admin("", ""),
                new EcoGestionProperties.Municipio(1.10, 1.32, -77.40, -77.18), false);
        servicio = new ReporteService(ciudadanoRepository, operadorRepository, reporteRepository, new ClasificacionService(),
                horarioRutaService, notificacionService, servicioMapas, almacenamiento, propiedades);

        ciudadano = new Ciudadano("Ana", "123", "ana@correo.co", "hash", null);
        ReflectionTestUtils.setField(ciudadano, "id", "c1");
        operador = new Operador("Luis", "456", "luis@correo.co", "hash");
        ReflectionTestUtils.setField(operador, "id", "o1");
        when(ciudadanoRepository.findById("c1")).thenReturn(Optional.of(ciudadano));
        when(operadorRepository.findById("o1")).thenReturn(Optional.of(operador));
        when(reporteRepository.save(any(Reporte.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Reporte reportePendiente() {
        Reporte r = new Reporte(LocalDateTime.now(), ciudadano, "Escombros en el andén");
        ReflectionTestUtils.setField(r, "id", "r1");
        r.clasificar(TipoResiduo.ESCOMBROS, Prioridad.MEDIA);
        when(reporteRepository.findById("r1")).thenReturn(Optional.of(r));
        return r;
    }

    @Test
    @DisplayName("HU-03/04/05: el reporte se crea Pendiente, con prioridad sugerida, dirección, sector y fotos")
    void registrarReporte() {
        when(servicioMapas.obtenerDireccion(1.2136, -77.2811)).thenReturn(Optional.of("Calle 18 # 24"));
        when(horarioRutaService.identificarSector(1.2136, -77.2811)).thenReturn(Optional.empty());
        when(almacenamiento.guardar(any())).thenReturn(List.of("/api/imagenes/abc"));

        ReporteResponse r = servicio.registrarReporte(
                new ReporteRequest("Residuos de pintura", TipoResiduo.PELIGROSO, 1.2136, -77.2811), List.of(), "c1");

        assertThat(r.estado()).isEqualTo(EstadoReporte.PENDIENTE);
        assertThat(r.prioridad()).isEqualTo(Prioridad.ALTA);
        assertThat(r.direccion()).isEqualTo("Calle 18 # 24");
        assertThat(r.fotos()).containsExactly("/api/imagenes/abc");
    }

    @Test
    @DisplayName("HU-04: una ubicación fuera de Pasto se rechaza")
    void ubicacionFueraDePasto() {
        assertThatThrownBy(() -> servicio.registrarReporte(
                new ReporteRequest("Basura", TipoResiduo.DOMESTICO, 4.7110, -74.0721), List.of(), "c1"))
                .isInstanceOf(DatosInvalidosException.class);
        verify(almacenamiento, never()).guardar(any());
    }

    @Test
    @DisplayName("HU-07 y HU-11: asignar pasa el reporte a Asignado y notifica al ciudadano")
    void asignarNotifica() {
        reportePendiente();
        ReporteResponse r = servicio.asignarReporte("r1", "o1", false);
        assertThat(r.estado()).isEqualTo(EstadoReporte.ASIGNADO);
        assertThat(r.operador().nombre()).isEqualTo("Luis");
        verify(notificacionService).notificarCambioEstado(any(Reporte.class));
    }

    @Test
    @DisplayName("HU-07: no se asigna a un operador no disponible")
    void operadorNoDisponible() {
        reportePendiente();
        operador.marcarOcupado();
        assertThatThrownBy(() -> servicio.asignarReporte("r1", "o1", false))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("no está disponible");
    }

    @Test
    @DisplayName("HU-07: dos asignaciones simultáneas no se guardan (control de versión)")
    void asignacionConcurrente() {
        reportePendiente();
        when(reporteRepository.save(any(Reporte.class))).thenThrow(new OptimisticLockingFailureException("versión"));
        assertThatThrownBy(() -> servicio.asignarReporte("r1", "o1", false)).isInstanceOf(ReglaNegocioException.class);
        verify(notificacionService, never()).notificarCambioEstado(any());
    }
}
