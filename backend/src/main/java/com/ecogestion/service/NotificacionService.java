package com.ecogestion.service;

import com.ecogestion.config.EcoGestionProperties;
import com.ecogestion.dto.ContenidoDto.NotificacionResponse;
import com.ecogestion.integracion.notificacion.CanalNotificacion;
import com.ecogestion.integracion.notificacion.EnvioNotificacionException;
import com.ecogestion.model.EstadoReporte;
import com.ecogestion.model.Notificacion;
import com.ecogestion.model.Reporte;
import com.ecogestion.repository.NotificacionRepository;
import com.ecogestion.repository.ReporteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Componente "Notificaciones". Provee "Notificar cambio de estado" (a Asignación y atención) y requiere
 * "Obtener correo del ciudadano" (de Gestión de usuarios) y la interfaz externa "Canal de notificación".
 * Historia HU-11.
 */
@Service
public class NotificacionService {

    private static final Logger LOG = LoggerFactory.getLogger(NotificacionService.class);

    private final NotificacionRepository notificacionRepository;
    private final ReporteRepository reporteRepository;
    private final UsuarioService usuarioService;
    private final CanalNotificacion canal;
    private final int reintentos;

    public NotificacionService(NotificacionRepository notificacionRepository, ReporteRepository reporteRepository,
                               UsuarioService usuarioService, CanalNotificacion canal, EcoGestionProperties propiedades) {
        this.notificacionRepository = notificacionRepository;
        this.reporteRepository = reporteRepository;
        this.usuarioService = usuarioService;
        this.canal = canal;
        this.reintentos = Math.max(1, propiedades.notificaciones().reintentos());
    }

    /**
     * Notifica al ciudadano del reporte, y a los ciudadanos de sus duplicados, cuando el reporte pasa a
     * Asignado, En proceso o Resuelto (HU-11, criterios 1 y 2; HU-06, criterio 3).
     */
    public List<Notificacion> notificarCambioEstado(Reporte reporte) {
        EstadoReporte estado = reporte.getEstado();
        if (estado == EstadoReporte.PENDIENTE) {
            return List.of();
        }
        List<Reporte> afectados = new ArrayList<>();
        afectados.add(reporte);
        afectados.addAll(reporteRepository.findByReporteOriginal(reporte));
        List<Notificacion> enviadas = new ArrayList<>();
        for (Reporte r : afectados) {
            String ciudadanoId = r.getCiudadano().getId();
            String correo = usuarioService.obtenerCorreoDelCiudadano(ciudadanoId);
            Notificacion notificacion = new Notificacion(ciudadanoId, correo, asunto(reporte, r), mensaje(reporte, r),
                    r.getId(), estado);
            enviar(notificacion);
            enviadas.add(notificacionRepository.save(notificacion));
        }
        return enviadas;
    }

    /** Historial de notificaciones del usuario (HU-11, criterio 4). */
    public List<NotificacionResponse> historial(String usuarioId) {
        return notificacionRepository.findByDestinatarioIdOrderByFechaCreacionDesc(usuarioId).stream()
                .map(NotificacionResponse::de).toList();
    }

    /** Intenta el envío hasta el número de reintentos configurado (HU-11, criterio 3). */
    private void enviar(Notificacion notificacion) {
        for (int intento = 1; intento <= reintentos; intento++) {
            notificacion.registrarIntento();
            try {
                canal.enviar(notificacion);
                notificacion.marcarEnviada(canal.nombre());
                return;
            } catch (EnvioNotificacionException e) {
                LOG.warn("Intento {} de {} fallido: {}", intento, reintentos, e.getMessage());
            }
        }
    }

    private static String asunto(Reporte original, Reporte destino) {
        return "EcoGestión – Tu reporte " + destino.getId() + " está " + texto(original.getEstado());
    }

    private static String mensaje(Reporte original, Reporte destino) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hola ").append(destino.getCiudadano().getNombre()).append(",\n\n")
                .append("Tu reporte ").append(destino.getId()).append(" (").append(destino.getDescripcion()).append(")")
                .append(" cambió de estado a: ").append(texto(original.getEstado())).append(".\n");
        if (destino != original) {
            sb.append("Tu reporte fue vinculado al reporte ").append(original.getId())
                    .append(" porque corresponde al mismo problema.\n");
        }
        if (original.getEstado() == EstadoReporte.ASIGNADO && original.getOperador() != null) {
            sb.append("Operador responsable: ").append(original.getOperador().getNombre()).append(".\n");
        }
        if (original.getEstado() == EstadoReporte.RESUELTO) {
            sb.append("Trabajo realizado: ").append(original.getDescripcionTrabajo()).append("\n");
        }
        return sb.append("\nGracias por ayudar a mantener limpia a Pasto.").toString();
    }

    private static String texto(EstadoReporte estado) {
        return switch (estado) {
            case PENDIENTE -> "Pendiente";
            case ASIGNADO -> "Asignado";
            case EN_PROCESO -> "En proceso";
            case RESUELTO -> "Resuelto";
        };
    }
}
