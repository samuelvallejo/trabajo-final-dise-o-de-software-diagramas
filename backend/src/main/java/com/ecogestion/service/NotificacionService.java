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
 * Lógica del componente "Notificaciones" (HU-11): avisa al ciudadano cuando su reporte pasa a
 * Asignado, En proceso o Resuelto. Si el reporte tiene duplicados, también avisa a los ciudadanos
 * que crearon esos duplicados (HU-06).
 *
 * <p><b>Cómo funciona:</b> por cada destinatario crea una {@link Notificacion}, intenta enviarla por el
 * canal configurado hasta 3 veces y la guarda siempre (enviada o no) para que quede en el historial.
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Creador:</b> crea las {@link Notificacion} porque tiene todos los datos para armarlas
 *       (reporte, destinatario, asunto y mensaje).</li>
 *   <li><b>Polimorfismo + Variaciones protegidas:</b> envía a través de la interfaz
 *       {@link CanalNotificacion}. Hoy hay un canal de correo y uno de registro; agregar SMS o push es crear
 *       otra clase que implemente la interfaz, sin tocar este servicio.</li>
 *   <li><b>Indirección:</b> no habla con el servidor SMTP; el canal hace de intermediario.</li>
 * </ul>
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

    /** Servicio "Notificar cambio de estado" (lo usa Asignación y atención). Devuelve las notificaciones creadas. */
    public List<Notificacion> notificarCambioEstado(Reporte reporte) {
        if (reporte.getEstado() == EstadoReporte.PENDIENTE) {
            return List.of(); // pasar a Pendiente no genera aviso
        }
        // Destinatarios: el reporte original y todos sus duplicados
        List<Reporte> afectados = new ArrayList<>();
        afectados.add(reporte);
        afectados.addAll(reporteRepository.findByReporteOriginal(reporte));

        List<Notificacion> creadas = new ArrayList<>();
        for (Reporte afectado : afectados) {
            String ciudadanoId = afectado.getCiudadano().getId();
            Notificacion notificacion = new Notificacion(ciudadanoId,
                    usuarioService.obtenerCorreoDelCiudadano(ciudadanoId),
                    asunto(reporte, afectado), mensaje(reporte, afectado), afectado.getId(), reporte.getEstado());
            enviarConReintentos(notificacion);
            creadas.add(notificacionRepository.save(notificacion));
        }
        return creadas;
    }

    /** Historial de notificaciones del usuario, de la más reciente a la más antigua (HU-11, criterio 4). */
    public List<NotificacionResponse> historial(String usuarioId) {
        return notificacionRepository.findByDestinatarioIdOrderByFechaCreacionDesc(usuarioId).stream()
                .map(NotificacionResponse::de)
                .toList();
    }

    /** Intenta enviar hasta {@code reintentos} veces; si todas fallan, la notificación queda como no enviada. */
    private void enviarConReintentos(Notificacion notificacion) {
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

    /** Arma el texto del aviso; agrega datos según el estado (operador asignado o trabajo realizado). */
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
