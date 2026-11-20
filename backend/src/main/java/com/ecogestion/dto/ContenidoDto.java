package com.ecogestion.dto;

import com.ecogestion.model.CategoriaContenido;
import com.ecogestion.model.ContenidoEducativo;
import com.ecogestion.model.EstadoReporte;
import com.ecogestion.model.Notificacion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Objetos de transferencia (DTO) de Contenido educativo (HU-14) y Notificaciones (HU-11).
 *
 * <p><b>Cómo funciona:</b> cada {@code record} es un objeto inmutable con los datos que entran
 * ({@code ...Request}, validados con anotaciones como {@code @NotBlank}) o que salen ({@code ...Response})
 * de la API en formato JSON. Los métodos {@code de(...)} convierten un objeto del modelo en su respuesta.
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Bajo acoplamiento:</b> la API no expone las clases del modelo; si cambia un documento de
 *       MongoDB, el JSON que usa el frontend no cambia. Tampoco se envían datos sensibles
 *       (por ejemplo el hash de la contraseña).</li>
 *   <li><b>Creador:</b> cada respuesta sabe construirse a partir de su objeto del modelo ({@code de}).</li>
 * </ul>
 */
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
