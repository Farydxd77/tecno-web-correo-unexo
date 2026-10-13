package com.corre_email.unexo.validation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parametros ya validados y normalizados, accesibles por nombre. Los lectores
 * con tipo (id, entero, fecha...) devuelven null si el parametro vino vacio;
 * no fallan porque las reglas ya comprobaron el formato.
 */
public final class Parametros {

    private final Map<String, String> valores = new LinkedHashMap<>();

    void poner(String nombre, String valor) {
        valores.put(nombre, valor);
    }

    /** El valor del parametro; "" si era opcional y vino vacio o no vino. */
    public String get(String nombre) {
        if (!valores.containsKey(nombre)) {
            throw new IllegalArgumentException("El comando no declara el parametro '" + nombre + "'");
        }
        return valores.get(nombre);
    }

    /** Igual que get, pero null si vino vacio (para guardar NULL en la base de datos). */
    public String opcional(String nombre) {
        String valor = get(nombre);
        return valor.isEmpty() ? null : valor;
    }

    public boolean esComodin(String nombre) {
        return "*".equals(get(nombre));
    }

    public Long id(String nombre) {
        String valor = opcional(nombre);
        return valor == null ? null : Long.valueOf(valor);
    }

    public Integer entero(String nombre) {
        String valor = opcional(nombre);
        return valor == null ? null : Integer.valueOf(valor);
    }

    public BigDecimal decimal(String nombre) {
        String valor = opcional(nombre);
        return valor == null ? null : new BigDecimal(valor);
    }

    public LocalDate fecha(String nombre) {
        String valor = opcional(nombre);
        return valor == null ? null : LocalDate.parse(valor);
    }

    public LocalTime hora(String nombre) {
        String valor = opcional(nombre);
        return valor == null ? null : LocalTime.parse(valor);
    }

    /** Para parametros declarados con Reglas.unoDe("SI", "NO"). */
    public Boolean siNo(String nombre) {
        String valor = opcional(nombre);
        return valor == null ? null : valor.equals("SI");
    }
}
