package com.ecogestion.config;

import com.ecogestion.integracion.mapas.CoordenadasServicioMapas;
import com.ecogestion.integracion.mapas.OpenStreetMapServicioMapas;
import com.ecogestion.integracion.mapas.ServicioMapas;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elige al arrancar qué implementación de {@link ServicioMapas} usar, según la variable
 * MAPAS_GEOCODIFICACION: {@code osm} (por defecto) consulta la dirección a OpenStreetMap (Nominatim);
 * {@code coordenadas} no usa ningún servicio externo y arma la dirección con las coordenadas
 * (útil sin conexión a internet).
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
        if (propiedades.mapas() != null && propiedades.mapas().usaOpenStreetMap()) {
            return new OpenStreetMapServicioMapas();
        }
        return new CoordenadasServicioMapas();
    }
}
