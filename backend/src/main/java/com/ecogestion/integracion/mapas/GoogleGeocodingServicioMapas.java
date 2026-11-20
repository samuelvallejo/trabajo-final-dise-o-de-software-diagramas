package com.ecogestion.integracion.mapas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementación con la Geocoding API de Google Maps Platform (HTTPS:443 en la vista de despliegue).
 * Se usa cuando está definida la variable GOOGLE_MAPS_API_KEY. Si Google falla, devuelve vacío y el
 * reporte se guarda igual, sin dirección.
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Polimorfismo:</b> implementación concreta de {@link ServicioMapas}.</li>
 * </ul>
 */
public class GoogleGeocodingServicioMapas implements ServicioMapas {

    private static final Logger LOG = LoggerFactory.getLogger(GoogleGeocodingServicioMapas.class);

    private final RestClient cliente;
    private final String claveApi;

    public GoogleGeocodingServicioMapas(String claveApi) {
        this.claveApi = claveApi;
        this.cliente = RestClient.create("https://maps.googleapis.com/maps/api/geocode/json");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<String> obtenerDireccion(double latitud, double longitud) {
        try {
            Map<String, Object> respuesta = cliente.get()
                    .uri(uri -> uri.queryParam("latlng", latitud + "," + longitud)
                            .queryParam("language", "es")
                            .queryParam("key", claveApi)
                            .build())
                    .retrieve()
                    .body(Map.class);
            if (respuesta == null || !(respuesta.get("results") instanceof List<?> resultados) || resultados.isEmpty()) {
                return Optional.empty();
            }
            Object primero = resultados.get(0);
            if (primero instanceof Map<?, ?> resultado && resultado.get("formatted_address") instanceof String direccion) {
                return Optional.of(direccion);
            }
            return Optional.empty();
        } catch (RestClientException e) {
            LOG.warn("No se pudo geocodificar ({}, {}): {}", latitud, longitud, e.getMessage());
            return Optional.empty();
        }
    }
}
