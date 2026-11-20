package com.ecogestion.config;

import com.ecogestion.integracion.mapas.CoordenadasServicioMapas;
import com.ecogestion.integracion.mapas.GoogleGeocodingServicioMapas;
import com.ecogestion.integracion.mapas.ServicioMapas;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elige la implementación del servicio de mapas según la configuración.
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
