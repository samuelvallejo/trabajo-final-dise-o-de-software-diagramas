package com.ecogestion.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Seguridad con Spring Security: sesión HTTP (cookie SameSite=Strict), contraseñas con BCrypt y
 * autorización por rol según las historias de usuario.
 */
@Configuration
public class SeguridadConfig {

    private static final String CIUDADANO = "CIUDADANO";
    private static final String ADMINISTRADOR = "ADMINISTRADOR";
    private static final String OPERADOR = "OPERADOR";

    @Bean
    public SecurityFilterChain filtroSeguridad(HttpSecurity http, SecurityContextRepository repositorioContexto)
            throws Exception {
        http
                // API REST consumida con sesión SameSite=Strict; las páginas Thymeleaf conservan CSRF.
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .securityContext(sc -> sc.securityContextRepository(repositorioContexto))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/", "/error", "/css/**", "/favicon.ico", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/registro", "/api/auth/login").permitAll()
                        // Consultas públicas (HU-12, HU-13, HU-14)
                        .requestMatchers("/api/horarios/mi-sector", "/api/puntos/mi-sector").hasRole(CIUDADANO)
                        .requestMatchers(HttpMethod.GET, "/api/sectores/**", "/api/horarios/**", "/api/puntos/**",
                                "/api/contenidos/**", "/api/clasificacion/**").permitAll()
                        // Roles
                        .requestMatchers("/api/admin/**").hasRole(ADMINISTRADOR)
                        .requestMatchers("/api/operador/**").hasRole(OPERADOR)
                        .requestMatchers("/api/ciudadanos/**").hasRole(CIUDADANO)
                        .requestMatchers(HttpMethod.POST, "/api/reportes").hasRole(CIUDADANO)
                        .requestMatchers("/api/reportes/*/asignacion", "/api/reportes/*/clasificacion",
                                "/api/reportes/*/duplicado", "/api/reportes/*/posibles-duplicados").hasRole(ADMINISTRADOR)
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .logout(l -> l.logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)));
        return http.build();
    }

    @Bean
    public SecurityContextRepository repositorioContexto() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuracion) throws Exception {
        return configuracion.getAuthenticationManager();
    }
}
