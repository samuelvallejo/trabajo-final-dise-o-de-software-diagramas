package com.ecogestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parámetros de negocio que se cambian sin tocar el código (prefijo {@code ecogestion} en
 * application.properties o variables de entorno): radio de duplicados, intentos de inicio de sesión,
 * límites del municipio, canal de notificación, etc.
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Variaciones protegidas:</b> los valores que pueden cambiar están fuera del código.</li>
 *   <li><b>Experto en información:</b> {@link Municipio} sabe si una coordenada está dentro de Pasto ({@code contiene}).</li>
 * </ul>
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

    /** geocodificacion: "osm" (OpenStreetMap Nominatim) o "coordenadas" (sin servicio externo). */
    public record Mapas(String geocodificacion) {
        public boolean usaOpenStreetMap() {
            return !"coordenadas".equalsIgnoreCase(geocodificacion);
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
