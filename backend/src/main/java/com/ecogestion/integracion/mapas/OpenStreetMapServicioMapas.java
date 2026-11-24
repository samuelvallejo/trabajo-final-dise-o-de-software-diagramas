package com.ecogestion.integracion.mapas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.Optional;

/**
 * Obtiene la dirección aproximada de unas coordenadas con Nominatim, el servicio de geocodificación de
 * OpenStreetMap (HTTPS:443 en la vista de despliegue). No necesita clave. Si el servicio no responde,
 * devuelve vacío y el reporte se guarda igual, sin dirección.
 *
 * <p>Las normas de uso de Nominatim piden identificar la aplicación en el encabezado User-Agent y no
 * hacer más de una consulta por segundo; un reporte genera una sola consulta.
 *
 * <p><b>Patrón GRASP — Polimorfismo:</b> implementación concreta de {@link ServicioMapas}.
 */
public class OpenStreetMapServicioMapas implements ServicioMapas {

    private static final Logger LOG = LoggerFactory.getLogger(OpenStreetMapServicioMapas.class);

    private final RestClient cliente = RestClient.builder()
            .baseUrl("https://nominatim.openstreetmap.org/reverse")
            .defaultHeader(HttpHeaders.USER_AGENT, "EcoGestion/1.0 (proyecto academico, Pasto)")
            .build();

    @Override
    public Optional<String> obtenerDireccion(double latitud, double longitud) {
        try {
            Map<?, ?> respuesta = cliente.get()
                    .uri(uri -> uri.queryParam("format", "jsonv2")
                            .queryParam("lat", latitud)
                            .queryParam("lon", longitud)
                            .queryParam("accept-language", "es")
                            .build())
                    .retrieve()
                    .body(Map.class);
            if (respuesta != null && respuesta.get("display_name") instanceof String direccion) {
                return Optional.of(direccion);
            }
            return Optional.empty();
        } catch (RestClientException e) {
            LOG.warn("No se pudo geocodificar ({}, {}): {}", latitud, longitud, e.getMessage());
            return Optional.empty();
        }
    }
}
