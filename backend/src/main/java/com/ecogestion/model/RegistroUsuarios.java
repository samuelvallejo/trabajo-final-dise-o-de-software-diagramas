package com.ecogestion.model;

/**
 * Dónde se guarda cada tipo de usuario (cada uno vive en su propia colección de MongoDB).
 * La implementa {@code UsuarioService} con sus tres repositorios.
 *
 * <p><b>Patrones GRASP:</b>
 * <ul>
 *   <li><b>Polimorfismo (despacho doble):</b> el servicio llama {@code usuario.guardarEn(registro)} y cada
 *       subclase de {@link Usuario} elige el método {@code guardar} que le corresponde. Así no hace falta
 *       preguntar con {@code switch} o {@code instanceof} de qué tipo es el usuario.</li>
 *   <li><b>Indirección:</b> el modelo no conoce los repositorios; solo esta interfaz.</li>
 * </ul>
 */
public interface RegistroUsuarios {

    void guardar(Ciudadano ciudadano);

    void guardar(Operador operador);

    void guardar(Administrador administrador);
}
