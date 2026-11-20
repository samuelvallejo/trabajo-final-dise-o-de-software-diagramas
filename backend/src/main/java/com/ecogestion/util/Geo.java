package com.ecogestion.util;

/**
 * Cálculo de distancias entre coordenadas (fórmula del haversine), usado para mostrar a cuántos metros
 * está un posible duplicado (HU-06) o un punto de recolección (HU-13).
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Fabricación pura:</b> utilidad matemática sin estado que no pertenece a ninguna clase del dominio.</li>
 * </ul>
 */
public final class Geo {

    private static final double RADIO_TIERRA_METROS = 6_371_000;

    private Geo() {
    }

    /** Distancia en metros entre dos coordenadas (fórmula del haversine). */
    public static double distanciaMetros(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return Math.round(RADIO_TIERRA_METROS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)) * 10) / 10.0;
    }
}
