package com.ecogestion.integracion.imagenes;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

/**
 * Almacenamiento de las fotos de los reportes y de las evidencias (HU-03, HU-09).
 * Devuelve la URL con la que se consulta cada imagen; en el reporte solo se guardan las URL.
 */
public interface AlmacenamientoImagenes {

    String RUTA_PUBLICA = "/api/imagenes/";

    /** Valida y guarda las fotos; devuelve sus URL en el mismo orden. */
    List<String> guardar(List<MultipartFile> fotos);

    Optional<Imagen> obtener(String id);

    /** Imagen almacenada lista para enviarse al navegador. */
    record Imagen(String tipoContenido, byte[] contenido) { }
}
