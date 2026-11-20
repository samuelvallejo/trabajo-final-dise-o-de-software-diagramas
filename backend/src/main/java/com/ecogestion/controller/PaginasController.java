package com.ecogestion.controller;

import com.ecogestion.model.CategoriaContenido;
import com.ecogestion.model.EstadoReporte;
import com.ecogestion.model.MaterialAceptado;
import com.ecogestion.model.Prioridad;
import com.ecogestion.model.TipoResiduo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Entrega las páginas HTML del frontend (plantillas Thymeleaf en {@code resources/templates}):
 * <ul>
 *   <li>{@code /} — inicio: iniciar sesión, registrarse y consultar información pública.</li>
 *   <li>{@code /ciudadano} — crear reportes y ver su historial, notificaciones, horarios y perfil.</li>
 *   <li>{@code /administrador} — bandeja de reportes, operadores, estadísticas y contenido.</li>
 *   <li>{@code /operador} — reportes asignados y cierre con evidencias.</li>
 * </ul>
 * Las páginas solo traen la estructura; los datos se piden con JavaScript a la API REST
 * ({@code static/js}), que es la que valida la sesión y el rol.
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Controlador:</b> recibe la petición de cada página y decide qué plantilla mostrar.</li>
 *   <li><b>Bajo acoplamiento:</b> no usa servicios ni repositorios; solo envía a la plantilla los
 *       catálogos fijos (tipos de residuo, prioridades...) para armar las listas desplegables.</li>
 * </ul>
 */
@Controller
public class PaginasController {

    /** Estos catálogos quedan disponibles en todas las plantillas (por ejemplo {@code ${tipos}}). */
    @ModelAttribute("tipos")
    public TipoResiduo[] tipos() {
        return TipoResiduo.values();
    }

    @ModelAttribute("prioridades")
    public Prioridad[] prioridades() {
        return Prioridad.values();
    }

    @ModelAttribute("estados")
    public EstadoReporte[] estados() {
        return EstadoReporte.values();
    }

    @ModelAttribute("categorias")
    public CategoriaContenido[] categorias() {
        return CategoriaContenido.values();
    }

    @ModelAttribute("materiales")
    public MaterialAceptado[] materiales() {
        return MaterialAceptado.values();
    }

    @GetMapping("/")
    public String inicio() {
        return "index";
    }

    @GetMapping("/ciudadano")
    public String ciudadano() {
        return "ciudadano";
    }

    @GetMapping("/administrador")
    public String administrador() {
        return "administrador";
    }

    @GetMapping("/operador")
    public String operador() {
        return "operador";
    }
}
