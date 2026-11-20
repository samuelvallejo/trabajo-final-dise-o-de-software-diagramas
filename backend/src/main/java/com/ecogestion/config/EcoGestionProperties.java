package com.ecogestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parámetros de negocio configurables (prefijo {@code ecogestion}).
 */
@ConfigurationProperties(prefix = "ecogestion")
public record EcoGestionProperties(
        Notificaciones notificaciones,
        Mapas mapas,
        Duplicados duplicados,
        Seguridad seguridad,
        Admin admin,
        Municipio municipio,
        boolean datosDemo) {

    public record Notificaciones(String canal, String remitente, int reintentos) { }

    public record Mapas(String googleApiKey) {
        public boolean tieneClave() {
            return googleApiKey != null && !googleApiKey.isBlank();
        }
    }

    public record Duplicados(double radioMetros) { }

    public record Seguridad(int intentosMaximos, int minutosBloqueo) { }

    public record Admin(String correo, String contrasena) {
        public boolean configurado() {
            return correo != null && !correo.isBlank() && contrasena != null && !contrasena.isBlank();
        }
    }

    /** Rectángulo que delimita el municipio de Pasto (HU-04, criterio 4). */
    public record Municipio(double latitudMin, double latitudMax, double longitudMin, double longitudMax) {
        public boolean contiene(double latitud, double longitud) {
            return latitud >= latitudMin && latitud <= latitudMax
                    && longitud >= longitudMin && longitud <= longitudMax;
        }
    }
}
