package com.ecogestion.controller;

import com.ecogestion.excepcion.DatosInvalidosException;
import com.ecogestion.excepcion.RecursoNoEncontradoException;
import com.ecogestion.excepcion.ReglaNegocioException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

/**
 * Convierte las excepciones en respuestas HTTP con el formato estándar Problem Details (RFC 9457):
 * {@code {"status": 409, "title": "Regla de negocio", "detail": "El reporte ya está asignado a ..."}}.
 * El frontend muestra el campo {@code detail} al usuario.
 *
 * <p><b>Cómo funciona:</b> {@code @RestControllerAdvice} hace que Spring llame a estos métodos cuando
 * cualquier controlador lanza la excepción indicada en {@code @ExceptionHandler}.
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Fabricación pura:</b> no es un concepto del caso de estudio; existe para que ningún
 *       controlador tenga que repetir try/catch.</li>
 *   <li><b>Alta cohesión:</b> la traducción "error → código HTTP" está en un solo lugar.</li>
 *   <li><b>Bajo acoplamiento:</b> el dominio lanza excepciones propias (por ejemplo
 *       {@link ReglaNegocioException}) sin saber nada de HTTP.</li>
 * </ul>
 */
@RestControllerAdvice
public class ManejadorErrores {

    @ExceptionHandler(ReglaNegocioException.class)
    public ProblemDetail reglaNegocio(ReglaNegocioException e) {
        return problema(HttpStatus.CONFLICT, "Regla de negocio", e.getMessage());
    }

    @ExceptionHandler(DatosInvalidosException.class)
    public ProblemDetail datosInvalidos(DatosInvalidosException e) {
        return problema(HttpStatus.BAD_REQUEST, "Datos inválidos", e.getMessage());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ProblemDetail noEncontrado(RecursoNoEncontradoException e) {
        return problema(HttpStatus.NOT_FOUND, "No encontrado", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validacion(MethodArgumentNotValidException e) {
        String detalle = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getField)
                .distinct()
                .map(campo -> campo + ": " + e.getBindingResult().getFieldError(campo).getDefaultMessage())
                .collect(Collectors.joining("; "));
        return problema(HttpStatus.BAD_REQUEST, "Campos obligatorios o inválidos", detalle);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ProblemDetail formato(Exception e) {
        return problema(HttpStatus.BAD_REQUEST, "Formato inválido", "Revise el formato de los datos enviados");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail tamano(MaxUploadSizeExceededException e) {
        return problema(HttpStatus.BAD_REQUEST, "Archivo demasiado grande", "Cada foto puede pesar como máximo 5 MB");
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ProblemDetail duplicado(DuplicateKeyException e) {
        return problema(HttpStatus.CONFLICT, "Dato duplicado", "Ya existe un registro con esos datos");
    }

    @ExceptionHandler(LockedException.class)
    public ProblemDetail bloqueada(LockedException e) {
        return problema(HttpStatus.LOCKED, "Cuenta bloqueada",
                "La cuenta está bloqueada temporalmente por intentos fallidos. Intente de nuevo en 15 minutos");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail credenciales(BadCredentialsException e) {
        return problema(HttpStatus.UNAUTHORIZED, "Credenciales inválidas", "Correo o contraseña incorrectos");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail accesoDenegado(AccessDeniedException e) {
        return problema(HttpStatus.FORBIDDEN, "Acceso denegado", e.getMessage());
    }

    private static ProblemDetail problema(HttpStatus estado, String titulo, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(titulo);
        return problema;
    }
}
