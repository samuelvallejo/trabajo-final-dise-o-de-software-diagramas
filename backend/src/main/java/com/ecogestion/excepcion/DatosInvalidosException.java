package com.ecogestion.excepcion;

/**
 * Excepción del dominio: datos de entrada inválidos (campos vacíos, fotos incorrectas o ubicación fuera de Pasto). {@code ManejadorErrores} la convierte en HTTP 400.
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Bajo acoplamiento:</b> el modelo y los servicios lanzan excepciones propias sin conocer HTTP;
 *       la traducción a códigos de respuesta está en un solo lugar.</li>
 * </ul>
 */
public class DatosInvalidosException extends RuntimeException {

    public DatosInvalidosException(String mensaje) {
        super(mensaje);
    }
}
