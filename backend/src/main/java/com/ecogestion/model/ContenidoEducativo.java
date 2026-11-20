package com.ecogestion.model;

import com.ecogestion.excepcion.DatosInvalidosException;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Artículo de educación ambiental (colección {@code contenidos}, HU-14). Mientras no se publique es un
 * borrador que los ciudadanos no ven.
 *
 * <p><b>Patrón GRASP — Experto en información:</b> el contenido conoce su título y su texto, por eso él
 * mismo responde si contiene una palabra buscada ({@link #contiene}) y controla su publicación.
 */
@Document("contenidos")
public class ContenidoEducativo {

    @Id
    private String id;
    private String titulo;
    private CategoriaContenido categoria;
    private String cuerpo;
    private String imagenUrl;
    private boolean publicado;
    private LocalDateTime fechaPublicacion;
    private String autorId;

    protected ContenidoEducativo() {
    }

    public ContenidoEducativo(String titulo, CategoriaContenido categoria, String cuerpo, String imagenUrl,
                              String autorId) {
        actualizar(titulo, categoria, cuerpo, imagenUrl);
        this.autorId = autorId;
    }

    public void actualizar(String nuevoTitulo, CategoriaContenido nuevaCategoria, String nuevoCuerpo,
                           String nuevaImagen) {
        if (nuevoTitulo == null || nuevoTitulo.isBlank() || nuevoCuerpo == null || nuevoCuerpo.isBlank()) {
            throw new DatosInvalidosException("El título y el texto del contenido son obligatorios");
        }
        if (nuevaCategoria == null) {
            throw new DatosInvalidosException("La categoría es obligatoria");
        }
        this.titulo = nuevoTitulo.trim();
        this.categoria = nuevaCategoria;
        this.cuerpo = nuevoCuerpo.trim();
        this.imagenUrl = nuevaImagen;
    }

    public void publicar() {
        this.publicado = true;
        this.fechaPublicacion = LocalDateTime.now();
    }

    public void despublicar() {
        this.publicado = false;
    }

    /** Búsqueda por palabra clave en título o texto (HU-14, criterio 3). */
    public boolean contiene(String palabra) {
        String p = palabra.toLowerCase();
        return titulo.toLowerCase().contains(p) || cuerpo.toLowerCase().contains(p);
    }

    public String getId() { return id; }
    public String getTitulo() { return titulo; }
    public CategoriaContenido getCategoria() { return categoria; }
    public String getCuerpo() { return cuerpo; }
    public String getImagenUrl() { return imagenUrl; }
    public boolean isPublicado() { return publicado; }
    public LocalDateTime getFechaPublicacion() { return fechaPublicacion; }
    public String getAutorId() { return autorId; }
}
