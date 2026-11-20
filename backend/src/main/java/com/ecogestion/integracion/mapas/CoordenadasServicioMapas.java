package com.ecogestion.integracion.mapas;

import java.util.Locale;
import java.util.Optional;

/**
 * Implementación sin proveedor externo: usa las coordenadas como dirección aproximada.
 * Se usa cuando no hay clave de Google Maps, para que el sistema funcione igual.
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Polimorfismo:</b> implementación concreta de {@link ServicioMapas}.</li>
 * </ul>
 */
public class CoordenadasServicioMapas implements ServicioMapas {

    @Override
    public Optional<String> obtenerDireccion(double latitud, double longitud) {
        return Optional.of(String.format(Locale.ROOT, "Coordenadas %.6f, %.6f", latitud, longitud));
    }
}
