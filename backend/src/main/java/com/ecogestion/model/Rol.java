package com.ecogestion.model;

import java.util.List;

/**
 * Roles del sistema: son los tres actores de la vista de contexto. Spring Security usa
 * {@link #autoridad()} para decidir qué rutas puede usar cada uno.
 *
 * <p><b>Patrón GRASP — Polimorfismo:</b> cada rol implementa a su manera qué reportes ve y cuáles puede
 * abrir. En vez de un {@code switch (rol)} en el servicio, se llama {@code rol.puedeVer(...)} y cada
 * constante del enum responde con su propia regla. Agregar un rol nuevo obliga a escribir sus reglas
 * aquí, sin buscar {@code switch} repartidos por el código.
 */
public enum Rol {

    /** El ciudadano solo ve sus propios reportes (HU-10). */
    CIUDADANO {
        @Override
        public boolean puedeVer(Reporte reporte, String usuarioId) {
            return reporte.esDelCiudadano(usuarioId);
        }

        @Override
        public List<Reporte> reportesVisibles(CatalogoReportes catalogo, String usuarioId, EstadoReporte estado) {
            return catalogo.historialDelCiudadano(usuarioId);
        }
    },

    /** El administrador ve todos los reportes y trabaja con la bandeja de revisión (HU-05). */
    ADMINISTRADOR {
        @Override
        public boolean puedeVer(Reporte reporte, String usuarioId) {
            return true;
        }

        @Override
        public List<Reporte> reportesVisibles(CatalogoReportes catalogo, String usuarioId, EstadoReporte estado) {
            return catalogo.bandejaDeRevision(estado);
        }
    },

    /** El operador solo ve los reportes que tiene asignados (HU-08). */
    OPERADOR {
        @Override
        public boolean puedeVer(Reporte reporte, String usuarioId) {
            return reporte.estaAsignadoA(usuarioId);
        }

        @Override
        public List<Reporte> reportesVisibles(CatalogoReportes catalogo, String usuarioId, EstadoReporte estado) {
            return catalogo.pendientesDelOperador(usuarioId);
        }
    };

    /** Indica si un usuario con este rol puede abrir el detalle del reporte. */
    public abstract boolean puedeVer(Reporte reporte, String usuarioId);

    /** Lista de reportes que le corresponde ver a un usuario con este rol. */
    public abstract List<Reporte> reportesVisibles(CatalogoReportes catalogo, String usuarioId, EstadoReporte estado);

    /** Nombre de la autoridad en Spring Security (por convención lleva el prefijo ROLE_). */
    public String autoridad() {
        return "ROLE_" + name();
    }
}
