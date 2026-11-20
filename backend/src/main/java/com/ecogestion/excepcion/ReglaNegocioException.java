package com.ecogestion.excepcion;

/**
 * Se lanza cuando una operación viola una regla de negocio de las historias de usuario
 * (por ejemplo, asignar un reporte que ya tiene operador). Se responde con HTTP 409.
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
