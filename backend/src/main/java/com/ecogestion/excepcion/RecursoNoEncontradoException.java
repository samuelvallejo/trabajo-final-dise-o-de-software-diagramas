package com.ecogestion.excepcion;

/**
 * Excepción del dominio: el recurso no existe o el usuario no tiene permiso para verlo. {@code ManejadorErrores} la convierte en HTTP 404.
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Bajo acoplamiento:</b> el modelo y los servicios lanzan excepciones propias sin conocer HTTP;
 *       la traducción a códigos de respuesta está en un solo lugar.</li>
 * </ul>
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String recurso, String id) {
        super(recurso + " no encontrado: " + id);
    }
}
