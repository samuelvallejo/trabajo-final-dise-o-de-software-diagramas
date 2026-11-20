package com.ecogestion.dto;

import com.ecogestion.model.CategoriaContenido;
import com.ecogestion.model.ContenidoEducativo;
import com.ecogestion.model.EstadoReporte;
import com.ecogestion.model.Notificacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/** DTO de los módulos Contenido educativo (HU-14) y Notificaciones (HU-11). */
public final class ContenidoDto {

    private ContenidoDto() {
    }

    public record ContenidoRequest(
            @NotBlank String titulo,
            @NotNull CategoriaContenido categoria,
            @NotBlank String cuerpo,
            String imagenUrl,
            boolean publicar) { }

    public record ContenidoResponse(String id, String titulo, CategoriaContenido categoria, String cuerpo,
                                    String imagenUrl, boolean publicado, LocalDateTime fechaPublicacion) {
        public static ContenidoResponse de(ContenidoEducativo c) {
            return new ContenidoResponse(c.getId(), c.getTitulo(), c.getCategoria(), c.getCuerpo(), c.getImagenUrl(),
                    c.isPublicado(), c.getFechaPublicacion());
        }
    }

    public record NotificacionResponse(String id, String asunto, String mensaje, String reporteId,
                                       EstadoReporte estadoReporte, boolean enviada, int intentos, String canal,
                                       LocalDateTime fecha) {
        public static NotificacionResponse de(Notificacion n) {
            return new NotificacionResponse(n.getId(), n.getAsunto(), n.getMensaje(), n.getReporteId(),
                    n.getEstadoReporte(), n.isEnviada(), n.getIntentos(), n.getCanal(), n.getFechaCreacion());
        }
    }
}
