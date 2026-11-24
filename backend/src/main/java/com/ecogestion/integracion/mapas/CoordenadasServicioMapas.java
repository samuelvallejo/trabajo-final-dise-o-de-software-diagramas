package com.ecogestion.integracion.mapas;

import java.util.Locale;
import java.util.Optional;

/**
 * Implementación sin proveedor externo: usa las coordenadas como dirección aproximada.
 * Se usa con MAPAS_GEOCODIFICACION=coordenadas, por ejemplo para trabajar sin conexión a internet.
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
