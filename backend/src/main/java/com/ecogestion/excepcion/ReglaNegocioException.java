package com.ecogestion.excepcion;

/**
 * Excepción del dominio: la operación viola una regla de las historias, por ejemplo asignar un reporte que ya tiene operador. {@code ManejadorErrores} la convierte en HTTP 409.
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Bajo acoplamiento:</b> el modelo y los servicios lanzan excepciones propias sin conocer HTTP;
 *       la traducción a códigos de respuesta está en un solo lugar.</li>
 * </ul>
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
