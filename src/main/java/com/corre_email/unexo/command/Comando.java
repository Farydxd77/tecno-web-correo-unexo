package com.corre_email.unexo.command;

import com.corre_email.unexo.validation.DefinicionComando;
import com.corre_email.unexo.validation.Parametros;

import java.util.function.Function;

/**
 * Un comando armado con su definicion y la funcion que lo ejecuta. Permite que
 * cada tabla junte sus 5 comandos del CRUD en una sola clase (ver
 * ProgramaComandos) en vez de tener 5 clases casi iguales.
 */
public record Comando(DefinicionComando definicion, Function<Parametros, String> accion) implements CommandStrategy {

    @Override
    public String execute(Parametros params) {
        return accion.apply(params);
    }
}
