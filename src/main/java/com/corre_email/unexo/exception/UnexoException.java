package com.corre_email.unexo.exception;

import java.util.List;

/**
 * La unica excepcion del sistema. Lleva el {@link ErrorCode} y, segun el caso,
 * la posicion exacta del error en el asunto o la lista de errores de cada
 * parametro.
 */
public class UnexoException extends RuntimeException {

    private final ErrorCode error;

    /** Texto donde se encontro el error y posicion (base 0), para dibujar la flecha ^. */
    private String entrada;
    private int posicion = -1;

    /** Linea extra de ayuda, ej. "Formato correcto: INSPER[...]". */
    private String ayuda;

    /** Solo para ERRORES_EN_PARAMETROS: un error por cada parametro que fallo. */
    private List<UnexoException> errores = List.of();

    public UnexoException(ErrorCode error, String detalle) {
        super(detalle);
        this.error = error;
    }

    public UnexoException(ErrorCode error, String detalle, Throwable causa) {
        super(detalle, causa);
        this.error = error;
    }

    public UnexoException enPosicion(String entrada, int posicion) {
        this.entrada = entrada;
        this.posicion = posicion;
        return this;
    }

    public UnexoException conAyuda(String ayuda) {
        this.ayuda = ayuda;
        return this;
    }

    public UnexoException conErrores(List<UnexoException> errores) {
        this.errores = List.copyOf(errores);
        return this;
    }

    public ErrorCode getError() {
        return error;
    }

    public String getEntrada() {
        return entrada;
    }

    public int getPosicion() {
        return posicion;
    }

    public boolean tienePosicion() {
        return entrada != null && posicion >= 0;
    }

    public String getAyuda() {
        return ayuda;
    }

    public List<UnexoException> getErrores() {
        return errores;
    }
}
