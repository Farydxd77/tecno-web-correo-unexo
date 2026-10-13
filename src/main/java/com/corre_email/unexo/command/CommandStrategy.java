package com.corre_email.unexo.command;

import com.corre_email.unexo.validation.DefinicionComando;
import com.corre_email.unexo.validation.Parametros;

public interface CommandStrategy {

    /** Nombre, alias y parametros del comando. El dispatcher valida con esto antes de llamar a execute. */
    DefinicionComando definicion();

    /**
     * Recibe los parametros ya validados. Los errores de negocio (4xx) se lanzan
     * como UnexoException; no hace falta atrapar nada aqui.
     */
    String execute(Parametros params);
}
