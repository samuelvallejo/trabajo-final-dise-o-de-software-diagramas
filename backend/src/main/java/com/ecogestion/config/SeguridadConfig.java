package com.ecogestion.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
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
 * Configuración de seguridad con Spring Security.
 *
 * <p><b>Cómo funciona:</b>
 * <ol>
 *   <li>El usuario inicia sesión con {@code POST /api/auth/login}; su identidad queda guardada en la sesión
 *       HTTP y el navegador recibe la cookie {@code JSESSIONID} (SameSite=Strict, ver application.properties).</li>
 *   <li>En cada petición Spring Security lee la sesión y revisa las reglas de {@code authorizeHttpRequests}:
 *       qué rutas son públicas y qué rol necesita cada una.</li>
 *   <li>Si no hay sesión responde 401; si el rol no alcanza, 403.</li>
 * </ol>
 * Las páginas HTML son públicas porque solo contienen la interfaz; los datos siempre se piden a la API,
 * que sí está protegida.
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Fabricación pura:</b> clase técnica que concentra toda la seguridad en un solo lugar.</li>
 *   <li><b>Variaciones protegidas:</b> los servicios reciben un {@link PasswordEncoder} (interfaz); se puede
 *       cambiar BCrypt por otro algoritmo solo en este archivo.</li>
 * </ul>
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
                // La API se usa desde las páginas del mismo sitio con cookie SameSite=Strict, que ya impide
                // peticiones desde otros sitios; por eso no se exige el token CSRF en /api/**.
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .securityContext(sc -> sc.securityContextRepository(repositorioContexto))
                .authorizeHttpRequests(reglas -> reglas
                        // Páginas del frontend, archivos estáticos y salud del servicio (Railway)
                        .requestMatchers("/", "/ciudadano", "/administrador", "/operador", "/error",
                                "/css/**", "/js/**", "/favicon.ico", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/registro", "/api/auth/login").permitAll()
                        // Consultas del sector del propio ciudadano (necesitan saber quién es)
                        .requestMatchers("/api/horarios/mi-sector", "/api/puntos/mi-sector").hasRole(CIUDADANO)
                        // Consultas públicas: horarios, puntos, contenido educativo y tipos de residuo (HU-12, 13, 14)
                        .requestMatchers(HttpMethod.GET, "/api/sectores/**", "/api/horarios/**", "/api/puntos/**",
                                "/api/contenidos/**", "/api/clasificacion/**").permitAll()
                        // Rutas por rol
                        .requestMatchers("/api/admin/**").hasRole(ADMINISTRADOR)
                        .requestMatchers("/api/operador/**").hasRole(OPERADOR)
                        .requestMatchers("/api/ciudadanos/**").hasRole(CIUDADANO)
                        .requestMatchers(HttpMethod.POST, "/api/reportes").hasRole(CIUDADANO)
                        .requestMatchers("/api/reportes/*/asignacion", "/api/reportes/*/clasificacion",
                                "/api/reportes/*/duplicado", "/api/reportes/*/posibles-duplicados").hasRole(ADMINISTRADOR)
                        // Todo lo demás (por ejemplo GET /api/reportes) requiere haber iniciado sesión
                        .anyRequest().authenticated())
                // Sin sesión se responde 401 (en vez de redirigir a una página de login)
                .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .logout(salida -> salida.logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)));
        return http.build();
    }

    /** Guarda la identidad del usuario en la sesión HTTP (lo usa AuthController al iniciar sesión). */
    @Bean
    public SecurityContextRepository repositorioContexto() {
        return new HttpSessionSecurityContextRepository();
    }

    /** Las contraseñas se guardan cifradas con BCrypt (nunca en texto plano). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Componente de Spring que comprueba correo y contraseña usando {@code UsuarioDetailsService}. */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuracion) throws Exception {
        return configuracion.getAuthenticationManager();
    }
}
