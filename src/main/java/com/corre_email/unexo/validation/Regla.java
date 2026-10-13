package com.corre_email.unexo.validation;

/**
 * Una validacion sobre el valor de un parametro. Si el valor es invalido lanza
 * una UnexoException (3xx); si es valido devuelve el valor, que puede venir
 * normalizado (ej. "docente" pasa a "Docente").
 */
@FunctionalInterface
public interface Regla {

    /**
     * @param valor    el valor ya recortado y no vacio
     * @param etiqueta texto para los mensajes, ej. "Parametro 1 (ci)"
     */
    String aplicar(String valor, String etiqueta);
}
