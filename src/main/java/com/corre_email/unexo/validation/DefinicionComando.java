package com.corre_email.unexo.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lo que un comando declara sobre si mismo: nombre, alias, descripcion y
 * parametros. De aqui salen la validacion generica, los mensajes de error y
 * el texto de HELP.
 */
public final class DefinicionComando {

    private final String nombre;
    private final String descripcion;
    private final List<String> alias = new ArrayList<>();
    private final List<ParamDef> parametros = new ArrayList<>();
    private String ejemplo;

    private DefinicionComando(String nombre, String descripcion) {
        this.nombre = nombre.toUpperCase();
        this.descripcion = descripcion;
    }

    public static DefinicionComando de(String nombre, String descripcion) {
        return new DefinicionComando(nombre, descripcion);
    }

    public DefinicionComando alias(String... otros) {
        for (String otro : otros) {
            alias.add(otro.toUpperCase());
        }
        return this;
    }

    public DefinicionComando param(ParamDef parametro) {
        parametros.add(parametro);
        return this;
    }

    public DefinicionComando ejemplo(String ejemplo) {
        this.ejemplo = ejemplo;
        return this;
    }

    public boolean responde(String comando) {
        return nombre.equalsIgnoreCase(comando) || alias.stream().anyMatch(a -> a.equalsIgnoreCase(comando));
    }

    /** Los parametros son posicionales: se exige hasta el ultimo obligatorio. */
    public int minimoParametros() {
        int minimo = 0;
        for (int i = 0; i < parametros.size(); i++) {
            if (parametros.get(i).obligatorio()) {
                minimo = i + 1;
            }
        }
        return minimo;
    }

    public int maximoParametros() {
        return parametros.size();
    }

    /** Ej: INSPER["ci","nombre","apellido"] o LISPER["*"] */
    public String formato() {
        if (parametros.isEmpty()) {
            return nombre;
        }
        return parametros.stream()
                .map(p -> "\"" + (p.aceptaComodin() ? "*" : p.nombre()) + "\"")
                .collect(Collectors.joining(",", nombre + "[", "]"));
    }

    /** Linea de ayuda que acompaña a los errores de parametros. */
    public String ayuda() {
        return "Formato correcto: " + formato() + (ejemplo != null ? "\nEjemplo: " + ejemplo : "");
    }

    public String nombre() {
        return nombre;
    }

    public String descripcion() {
        return descripcion;
    }

    public List<String> alias() {
        return List.copyOf(alias);
    }

    public List<ParamDef> parametros() {
        return List.copyOf(parametros);
    }

    public String ejemplo() {
        return ejemplo;
    }
}
