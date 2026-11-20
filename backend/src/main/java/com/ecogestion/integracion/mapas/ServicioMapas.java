package com.ecogestion.integracion.mapas;

import java.util.Optional;

/**
 * Interfaz requerida "Servicio de mapas/GPS" de la vista funcional: obtiene la dirección aproximada de
 * unas coordenadas (geocodificación inversa, HU-04).
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Variaciones protegidas:</b> protege al sistema de los cambios del proveedor de mapas.</li>
 *   <li><b>Polimorfismo:</b> hay dos implementaciones (Google y solo coordenadas) y {@code ReporteService}
 *       no sabe cuál está usando.</li>
 * </ul>
 */
public interface ServicioMapas {

    Optional<String> obtenerDireccion(double latitud, double longitud);
}
