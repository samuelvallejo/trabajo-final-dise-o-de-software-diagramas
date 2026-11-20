package com.ecogestion.integracion.imagenes;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

/**
 * Interfaz del almacenamiento de fotos de reportes y de evidencias (HU-03, HU-09). Guarda los archivos y
 * devuelve la URL con la que se consulta cada uno; el {@code Reporte} solo guarda esas URL.
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Variaciones protegidas:</b> hoy las fotos van a MongoDB GridFS; pasar a otro almacenamiento
 *       (disco, nube) es crear otra implementación de esta interfaz.</li>
 *   <li><b>Indirección:</b> los servicios no saben cómo ni dónde se guardan los archivos.</li>
 * </ul>
 */
public interface AlmacenamientoImagenes {

    String RUTA_PUBLICA = "/api/imagenes/";

    /** Valida y guarda las fotos; devuelve sus URL en el mismo orden. */
    List<String> guardar(List<MultipartFile> fotos);

    Optional<Imagen> obtener(String id);

    /** Imagen almacenada lista para enviarse al navegador. */
    record Imagen(String tipoContenido, byte[] contenido) { }
}
