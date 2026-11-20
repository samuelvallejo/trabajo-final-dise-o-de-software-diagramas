package com.ecogestion.model;

import com.ecogestion.excepcion.DatosInvalidosException;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Ruta de recolección que cubre uno o varios sectores y tiene sus horarios (HU-12).
 * El recorrido es la lista de coordenadas que la vista web dibuja en el mapa.
 */
@Document("rutas")
public class RutaRecoleccion {

    @Id
    private String id;
    private String nombre;
    private List<String> sectores = new ArrayList<>();
    private List<HorarioRecoleccion> horarios = new ArrayList<>();
    private List<Coordenada> recorrido = new ArrayList<>();

    protected RutaRecoleccion() {
    }

    public RutaRecoleccion(String nombre, List<String> sectores, List<HorarioRecoleccion> horarios,
                           List<Coordenada> recorrido) {
        actualizar(nombre, sectores, horarios, recorrido);
    }

    /** Actualiza la ruta completa; los cambios se publican de inmediato (HU-12, criterio 3). */
    public void actualizar(String nuevoNombre, List<String> nuevosSectores, List<HorarioRecoleccion> nuevosHorarios,
                           List<Coordenada> nuevoRecorrido) {
        if (nuevoNombre == null || nuevoNombre.isBlank()) {
            throw new DatosInvalidosException("El nombre de la ruta es obligatorio");
        }
        if (nuevosSectores == null || nuevosSectores.isEmpty()) {
            throw new DatosInvalidosException("La ruta debe cubrir al menos un sector");
        }
        if (nuevosHorarios == null || nuevosHorarios.isEmpty()) {
            throw new DatosInvalidosException("La ruta debe tener al menos un horario");
        }
        this.nombre = nuevoNombre.trim();
        this.sectores = new ArrayList<>(nuevosSectores);
        this.horarios = nuevosHorarios.stream()
                .sorted(Comparator.comparing(HorarioRecoleccion::diaSemana).thenComparing(HorarioRecoleccion::horaInicio))
                .toList();
        this.recorrido = nuevoRecorrido == null ? new ArrayList<>() : new ArrayList<>(nuevoRecorrido);
    }

    public boolean cubreSector(String sector) {
        return sectores.stream().anyMatch(s -> s.equalsIgnoreCase(sector));
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }
    public List<String> getSectores() { return sectores; }
    public List<HorarioRecoleccion> getHorarios() { return horarios; }
    public List<Coordenada> getRecorrido() { return recorrido; }

    /** Punto del recorrido de la ruta. */
    public record Coordenada(double latitud, double longitud) { }
}
