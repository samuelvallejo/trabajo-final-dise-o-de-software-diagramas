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

/** Traduce las excepciones a respuestas HTTP con el formato Problem Details (RFC 9457). */
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
