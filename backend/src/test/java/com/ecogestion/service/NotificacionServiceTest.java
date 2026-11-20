package com.ecogestion.service;

import com.ecogestion.config.EcoGestionProperties;
import com.ecogestion.integracion.notificacion.CanalNotificacion;
import com.ecogestion.integracion.notificacion.EnvioNotificacionException;
import com.ecogestion.model.Ciudadano;
import com.ecogestion.model.Notificacion;
import com.ecogestion.model.Operador;
import com.ecogestion.model.Prioridad;
import com.ecogestion.model.Reporte;
import com.ecogestion.model.TipoResiduo;
import com.ecogestion.repository.NotificacionRepository;
import com.ecogestion.repository.ReporteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificacionServiceTest {

    private ReporteRepository reporteRepository;
    private UsuarioService usuarioService;
    private CanalNotificacion canal;
    private NotificacionService servicio;
    private Reporte reporte;

    @BeforeEach
    void preparar() {
        NotificacionRepository notificacionRepository = mock(NotificacionRepository.class);
        reporteRepository = mock(ReporteRepository.class);
        usuarioService = mock(UsuarioService.class);
        canal = mock(CanalNotificacion.class);
        when(canal.nombre()).thenReturn("correo");
        when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(i -> i.getArgument(0));
        EcoGestionProperties propiedades = new EcoGestionProperties(
                new EcoGestionProperties.Notificaciones("correo", "no-responder@ecogestion.co", 3), null, null, null, null, null, false);
        servicio = new NotificacionService(notificacionRepository, reporteRepository, usuarioService, canal, propiedades);

        Ciudadano ana = new Ciudadano("Ana", "1", "ana@correo.co", "h");
        ana.setId("c1");
        reporte = new Reporte(LocalDateTime.now(), ana, "Basura");
        reporte.setId("r1");
        reporte.clasificar(TipoResiduo.DOMESTICO, Prioridad.BAJA);
        Operador luis = new Operador("Luis", "2", "luis@correo.co", "h");
        reporte.asignar(luis);
        when(usuarioService.obtenerCorreoDelCiudadano("c1")).thenReturn("ana@correo.co");
    }

    @Test
    @DisplayName("HU-11: se notifica al ciudadano cuando el reporte se asigna")
    void notificaAsignacion() {
        when(reporteRepository.findByReporteOriginal(reporte)).thenReturn(List.of());
        List<Notificacion> enviadas = servicio.notificarCambioEstado(reporte);
        assertThat(enviadas).hasSize(1);
        assertThat(enviadas.get(0).isEnviada()).isTrue();
        assertThat(enviadas.get(0).getMensaje()).contains("Asignado").contains("Luis");
    }

    @Test
    @DisplayName("HU-11: si el canal falla se reintenta 3 veces y queda como no enviada")
    void reintentos() {
        when(reporteRepository.findByReporteOriginal(reporte)).thenReturn(List.of());
        doThrow(new EnvioNotificacionException("SMTP caído", null)).when(canal).enviar(any());
        Notificacion n = servicio.notificarCambioEstado(reporte).get(0);
        assertThat(n.isEnviada()).isFalse();
        assertThat(n.getIntentos()).isEqualTo(3);
        verify(canal, times(3)).enviar(any());
    }

    @Test
    @DisplayName("HU-06/HU-11: los ciudadanos de los duplicados también reciben la notificación")
    void notificaDuplicados() {
        Ciudadano pedro = new Ciudadano("Pedro", "3", "pedro@correo.co", "h");
        pedro.setId("c2");
        Reporte duplicado = new Reporte(LocalDateTime.now(), pedro, "Misma basura");
        duplicado.setId("r2");
        when(reporteRepository.findByReporteOriginal(reporte)).thenReturn(List.of(duplicado));
        when(usuarioService.obtenerCorreoDelCiudadano("c2")).thenReturn("pedro@correo.co");
        List<Notificacion> enviadas = servicio.notificarCambioEstado(reporte);
        assertThat(enviadas).extracting(Notificacion::getCorreoDestino).containsExactly("ana@correo.co", "pedro@correo.co");
    }
}
