package com.ecogestion.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Notificación de cambio de estado de un reporte. Queda en el historial del usuario y registra si se
 * envió y cuántos intentos se hicieron (HU-11).
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

    public String getId() { return id; }
    public String getDestinatarioId() { return destinatarioId; }
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
