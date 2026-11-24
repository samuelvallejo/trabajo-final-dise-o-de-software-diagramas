package com.ecogestion.service;

import com.ecogestion.dto.ContenidoDto.ContenidoRequest;
import com.ecogestion.dto.ContenidoDto.ContenidoResponse;
import com.ecogestion.excepcion.RecursoNoEncontradoException;
import com.ecogestion.model.CategoriaContenido;
import com.ecogestion.model.ContenidoEducativo;
import com.ecogestion.model.Rol;
import com.ecogestion.repository.ContenidoEducativoRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Lógica del componente "Contenido educativo" (HU-14): los ciudadanos leen lo publicado y el
 * administrador crea, edita, publica o retira contenidos.
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Creador:</b> crea los {@link ContenidoEducativo} con los datos del administrador.</li>
 *   <li><b>Bajo acoplamiento:</b> no decide quién es administrador; usa el servicio "Validar rol de
 *       administrador" de {@link UsuarioService}, como muestra la vista funcional.</li>
 *   <li><b>Experto en información:</b> la búsqueda por palabra y la publicación las hace el propio contenido.</li>
 * </ul>
 */
@Service
public class ContenidoEducativoService {

    private final ContenidoEducativoRepository contenidoRepository;
    private final UsuarioService usuarioService;

    public ContenidoEducativoService(ContenidoEducativoRepository contenidoRepository, UsuarioService usuarioService) {
        this.contenidoRepository = contenidoRepository;
        this.usuarioService = usuarioService;
    }

    /** Contenidos publicados, por categoría y palabra clave (HU-14, criterio 3). */
    public List<ContenidoResponse> listarPublicados(CategoriaContenido categoria, String palabraClave) {
        return contenidoRepository.findByPublicadoTrueOrderByFechaPublicacionDesc().stream()
                .filter(c -> categoria == null || c.getCategoria() == categoria)
                .filter(c -> palabraClave == null || palabraClave.isBlank() || c.contiene(palabraClave))
                .sorted(Comparator.comparing(ContenidoEducativo::getCategoria))
                .map(ContenidoResponse::de).toList();
    }

    public ContenidoResponse obtenerPublicado(String id) {
        ContenidoEducativo contenido = buscar(id);
        if (!contenido.isPublicado()) {
            throw new RecursoNoEncontradoException("Contenido", id);
        }
        return ContenidoResponse.de(contenido);
    }

    /** Todos los contenidos, incluidos borradores (solo administrador). */
    public List<ContenidoResponse> listarTodos(Rol rol) {
        usuarioService.validarRolAdministrador(rol);
        return contenidoRepository.findAll().stream().map(ContenidoResponse::de).toList();
    }

    /** Crea un contenido y, si se pide, lo publica (HU-14, criterio 1). */
    public ContenidoResponse crear(ContenidoRequest datos, String autorId, Rol rol) {
        usuarioService.validarRolAdministrador(rol);
        ContenidoEducativo contenido = new ContenidoEducativo(datos.titulo(), datos.categoria(), datos.cuerpo(),
                datos.imagenUrl(), autorId);
        if (datos.publicar()) {
            contenido.publicar();
        }
        return ContenidoResponse.de(contenidoRepository.save(contenido));
    }

    /** Edita un contenido; los cambios se reflejan de inmediato (HU-14, criterio 2). */
    public ContenidoResponse actualizar(String id, ContenidoRequest datos, Rol rol) {
        usuarioService.validarRolAdministrador(rol);
        ContenidoEducativo contenido = buscar(id);
        contenido.actualizar(datos.titulo(), datos.categoria(), datos.cuerpo(), datos.imagenUrl());
        if (datos.publicar()) {
            contenido.publicar();
        }
        return ContenidoResponse.de(contenidoRepository.save(contenido));
    }

    public ContenidoResponse cambiarPublicacion(String id, boolean publicar, Rol rol) {
        usuarioService.validarRolAdministrador(rol);
        ContenidoEducativo contenido = buscar(id);
        if (publicar) {
            contenido.publicar();
        } else {
            contenido.despublicar();
        }
        return ContenidoResponse.de(contenidoRepository.save(contenido));
    }

    private ContenidoEducativo buscar(String id) {
        return contenidoRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Contenido", id));
    }
}
