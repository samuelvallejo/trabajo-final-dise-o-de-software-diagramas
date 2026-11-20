package com.ecogestion.excepcion;

/**
 * Datos de entrada que no cumplen una validación (campos obligatorios, fotos, ubicación...).
 * Se responde con HTTP 400.
 */
public class DatosInvalidosException extends RuntimeException {

    public DatosInvalidosException(String mensaje) {
        super(mensaje);
    }
}
