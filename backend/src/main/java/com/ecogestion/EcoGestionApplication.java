package com.ecogestion;

import com.ecogestion.config.EcoGestionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * EcoGestión: reporte y gestión de residuos sólidos en Pasto (Nariño).
 */
@SpringBootApplication
@EnableConfigurationProperties(EcoGestionProperties.class)
public class EcoGestionApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcoGestionApplication.class, args);
    }
}
