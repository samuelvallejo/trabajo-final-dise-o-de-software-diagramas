package com.ecogestion.model;

import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

/**
 * Punto geográfico (latitud y longitud). Es un objeto de valor: inmutable y comparable por sus datos.
 * Lo usan la ubicación de los reportes, los puntos de recolección y el recorrido de las rutas.
 *
 * <p><b>Patrón GRASP — Experto en información:</b> la coordenada conoce su latitud y longitud, por eso
 * ella misma calcula la distancia a otra coordenada (fórmula del haversine) y se convierte al formato
 * GeoJSON que usa MongoDB. Antes esto era una función estática suelta en una clase de utilidades.
 */
public record Coordenada(double latitud, double longitud) {

    private static final double RADIO_TIERRA_METROS = 6_371_000;

    /** Distancia en metros hasta otra coordenada, redondeada a un decimal. */
    public double distanciaMetros(Coordenada otra) {
        double dLat = Math.toRadians(otra.latitud - latitud);
        double dLon = Math.toRadians(otra.longitud - longitud);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(latitud)) * Math.cos(Math.toRadians(otra.latitud))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double metros = RADIO_TIERRA_METROS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(metros * 10) / 10.0;
    }

    /** Punto GeoJSON para las consultas geográficas de MongoDB (GeoJSON usa el orden longitud, latitud). */
    public GeoJsonPoint aGeoJson() {
        return new GeoJsonPoint(longitud, latitud);
    }
}
