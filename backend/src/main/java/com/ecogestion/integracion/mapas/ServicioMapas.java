package com.ecogestion.integracion.mapas;

import java.util.Optional;

/**
 * Interfaz requerida "Servicio de mapas/GPS" de la vista funcional: obtiene la dirección aproximada
 * de unas coordenadas (geocodificación inversa, HU-04).
 */
public interface ServicioMapas {

    Optional<String> obtenerDireccion(double latitud, double longitud);
}
