package com.ecogestion.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Aviso que recibe un ciudadano cuando su reporte cambia de estado (colección {@code notificaciones}, HU-11).
 * Queda en su historial aunque el envío falle, y registra cuántos intentos se hicieron y por qué canal salió.
 *
 * <p><b>Patrón GRASP — Experto en información:</b> la notificación lleva la cuenta de sus propios
 * intentos y sabe marcarse como enviada.
 */
@Document("notificaciones")
public class Notificacion {

    @Id
    private String id;
    @Indexed
    private String destinatarioId;
    private String correoDestino;
    private String asunto;
    private String mensaje;
    private String reporteId;
    private EstadoReporte estadoReporte;
    private boolean enviada;
    private int intentos;
    private String canal;
    private LocalDateTime fechaCreacion;

    protected Notificacion() {
    }

    /**
     * Crea el aviso del cambio de estado de {@code reporte} para el ciudadano de {@code destino}.
     * {@code destino} es el mismo reporte o uno de sus duplicados (HU-06: los ciudadanos de los duplicados
     * también reciben el aviso).
     * <b>Creador / Experto en información:</b> la notificación sabe cómo se redacta a sí misma; el servicio
     * solo le pasa los datos.
     */
    public static Notificacion porCambioDeEstado(Reporte reporte, Reporte destino, String correoDestino) {
        EstadoReporte estado = reporte.getEstado();
        StringBuilder mensaje = new StringBuilder()
                .append("Hola ").append(destino.getCiudadano().getNombre()).append(",\n\n")
                .append("Tu reporte ").append(destino.getId()).append(" (").append(destino.getDescripcion()).append(")")
                .append(" cambió de estado a: ").append(estado.etiqueta()).append(".\n");
        if (destino != reporte) {
            mensaje.append("Tu reporte fue vinculado al reporte ").append(reporte.getId())
                    .append(" porque corresponde al mismo problema.\n");
        }
        mensaje.append(reporte.resumenDeAtencion())
                .append("\nGracias por ayudar a mantener limpia a Pasto.");
        return new Notificacion(destino.getCiudadano().getId(), correoDestino,
                "EcoGestión – Tu reporte " + destino.getId() + " está " + estado.etiqueta(),
                mensaje.toString(), destino.getId(), estado);
    }

    public Notificacion(String destinatarioId, String correoDestino, String asunto, String mensaje,
                        String reporteId, EstadoReporte estadoReporte) {
        this.destinatarioId = destinatarioId;
        this.correoDestino = correoDestino;
        this.asunto = asunto;
        this.mensaje = mensaje;
        this.reporteId = reporteId;
        this.estadoReporte = estadoReporte;
        this.fechaCreacion = LocalDateTime.now();
    }

    public void registrarIntento() {
        intentos++;
    }

    public void marcarEnviada(String nombreCanal) {
        this.enviada = true;
        this.canal = nombreCanal;
    }

    // Consultas de solo lectura: no hay setters (los cambios pasan por los métodos de arriba, que validan).
    // Los getters existen para armar las respuestas de la API.
    public String getId() { return id; }
    public String getCorreoDestino() { return correoDestino; }
    public String getAsunto() { return asunto; }
    public String getMensaje() { return mensaje; }
    public String getReporteId() { return reporteId; }
    public EstadoReporte getEstadoReporte() { return estadoReporte; }
    public boolean isEnviada() { return enviada; }
    public int getIntentos() { return intentos; }
    public String getCanal() { return canal; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
}
