package com.ecogestion;

import com.ecogestion.config.EcoGestionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Punto de entrada de EcoGestión: sistema de reporte y gestión de residuos sólidos en Pasto (Nariño).
 *
 * <p>Organización del código (vista de desarrollo) y patrón GRASP principal de cada paquete:
 * <pre>
 *   controller  → recibe las peticiones HTTP                         (Controlador)
 *   service     → casos de uso, uno por componente de la vista funcional (Alta cohesión, Creador)
 *   repository  → acceso a MongoDB                                    (Fabricación pura, Indirección)
 *   model       → clases del dominio con sus reglas                    (Experto en información, Polimorfismo)
 *   integracion → correo, mapas y fotos detrás de interfaces           (Variaciones protegidas)
 *   dto         → datos de entrada y salida de la API                  (Bajo acoplamiento)
 *   security    → inicio de sesión y roles con Spring Security
 *   config      → configuración y datos iniciales
 * </pre>
 */
@SpringBootApplication
@EnableConfigurationProperties(EcoGestionProperties.class)
public class EcoGestionApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcoGestionApplication.class, args);
    }
}
