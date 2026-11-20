package com.ecogestion.excepcion;

/**
 * El recurso solicitado no existe o el usuario no tiene permiso para verlo. Se responde con HTTP 404.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String recurso, String id) {
        super(recurso + " no encontrado: " + id);
    }
}
