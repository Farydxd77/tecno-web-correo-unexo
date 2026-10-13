package com.corre_email.unexo.validation;

import java.util.List;

/** Declaracion de un parametro de un comando: su nombre y que se exige de el. */
public record ParamDef(String nombre, boolean obligatorio, boolean aceptaComodin, List<Regla> reglas) {

    public static ParamDef obligatorio(String nombre, Regla... reglas) {
        return new ParamDef(nombre, true, false, List.of(reglas));
    }

    public static ParamDef opcional(String nombre, Regla... reglas) {
        return new ParamDef(nombre, false, false, List.of(reglas));
    }

    /** Permite "*" como valor (ej. LISPER["*"]); con "*" no se aplican las reglas. */
    public ParamDef conComodin() {
        return new ParamDef(nombre, obligatorio, true, reglas);
    }
}
