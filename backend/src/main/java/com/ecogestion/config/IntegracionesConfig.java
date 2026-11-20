package com.ecogestion.config;

import com.ecogestion.integracion.mapas.CoordenadasServicioMapas;
import com.ecogestion.integracion.mapas.GoogleGeocodingServicioMapas;
import com.ecogestion.integracion.mapas.ServicioMapas;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elige al arrancar qué implementación de {@link ServicioMapas} usar: Google Geocoding si existe la variable
 * GOOGLE_MAPS_API_KEY, o una versión sin proveedor externo si no existe.
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Polimorfismo y Variaciones protegidas:</b> el resto del sistema solo conoce la interfaz
 *       {@link ServicioMapas}; la decisión de qué proveedor usar está en un único lugar.</li>
 * </ul>
 */
@Configuration
public class IntegracionesConfig {

    @Bean
    public ServicioMapas servicioMapas(EcoGestionProperties propiedades) {
        if (propiedades.mapas() != null && propiedades.mapas().tieneClave()) {
            return new GoogleGeocodingServicioMapas(propiedades.mapas().googleApiKey());
        }
        return new CoordenadasServicioMapas();
    }
}
