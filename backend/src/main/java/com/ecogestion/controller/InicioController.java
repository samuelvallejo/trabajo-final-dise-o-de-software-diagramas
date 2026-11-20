package com.ecogestion.controller;

import com.ecogestion.model.TipoResiduo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Página de inicio (vista Thymeleaf) con la guía de la API. */
@Controller
public class InicioController {

    @GetMapping("/")
    public String inicio(Model modelo) {
        modelo.addAttribute("tipos", TipoResiduo.values());
        return "index";
    }
}
