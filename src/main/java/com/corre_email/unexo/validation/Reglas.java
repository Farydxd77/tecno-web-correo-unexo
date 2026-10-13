package com.corre_email.unexo.validation;

import com.corre_email.unexo.exception.ErrorCode;
import com.corre_email.unexo.exception.UnexoException;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Catalogo de reglas reutilizables para declarar los parametros de cada comando. */
public final class Reglas {

    private static final Pattern NUMERO = Pattern.compile("\\d+");
    private static final Pattern DECIMAL = Pattern.compile("\\d+([.,]\\d{1,2})?");
    private static final Pattern LETRAS = Pattern.compile("[\\p{L}]+([ '\\-][\\p{L}]+)*");
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+\\-]+@[A-Za-z0-9\\-]+(\\.[A-Za-z0-9\\-]+)*\\.[A-Za-z]{2,}");
    private static final Pattern FECHA_ISO = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern FECHA_LATINA = Pattern.compile("(\\d{1,2})/(\\d{1,2})/(\\d{4})");
    private static final Pattern HORA = Pattern.compile("(\\d{1,2}):(\\d{2})");

    private Reglas() {
    }

    /** Solo digitos: sin puntos, espacios, guiones ni letras. */
    public static Regla numero() {
        return (valor, etiqueta) -> {
            if (!NUMERO.matcher(valor).matches()) {
                throw new UnexoException(ErrorCode.TIPO_INCORRECTO,
                        etiqueta + ": \"" + valor + "\" debe contener solo numeros (sin puntos, espacios ni letras).");
            }
            return valor;
        };
    }

    /** Letras (con tildes y ñ), permitiendo espacios, apostrofes o guiones entre palabras. */
    public static Regla soloLetras() {
        return (valor, etiqueta) -> {
            if (!LETRAS.matcher(valor).matches()) {
                throw new UnexoException(ErrorCode.TIPO_INCORRECTO,
                        etiqueta + ": \"" + valor + "\" solo puede contener letras.");
            }
            return valor;
        };
    }

    public static Regla email() {
        return (valor, etiqueta) -> {
            if (EMAIL.matcher(valor).matches()) {
                return valor;
            }
            String motivo;
            long arrobas = valor.chars().filter(c -> c == '@').count();
            if (valor.contains(" ")) {
                motivo = "no puede tener espacios";
            } else if (arrobas == 0) {
                motivo = "le falta la @";
            } else if (arrobas > 1) {
                motivo = "tiene mas de una @";
            } else if (valor.startsWith("@")) {
                motivo = "le falta el usuario antes de la @";
            } else if (!valor.substring(valor.indexOf('@')).contains(".")) {
                motivo = "le falta el dominio despues de la @ (ej. tecnoweb.org.bo)";
            } else {
                motivo = "no es un email valido";
            }
            throw new UnexoException(ErrorCode.FORMATO_INCORRECTO,
                    etiqueta + ": \"" + valor + "\" " + motivo + ". Ejemplo: juan@tecnoweb.org.bo");
        };
    }

    public static Regla largo(int min, int max) {
        return (valor, etiqueta) -> {
            int n = valor.length();
            if (n < min || n > max) {
                String esperado = min == max
                        ? "exactamente " + min
                        : "entre " + min + " y " + max;
                throw new UnexoException(ErrorCode.LARGO_FUERA_DE_RANGO,
                        etiqueta + ": \"" + valor + "\" debe tener " + esperado + " caracteres y tiene " + n + ".");
            }
            return valor;
        };
    }

    /** Numero con hasta 2 decimales; acepta coma o punto y devuelve siempre con punto (1500,50 -> 1500.50). */
    public static Regla decimal() {
        return (valor, etiqueta) -> {
            if (!DECIMAL.matcher(valor).matches()) {
                throw new UnexoException(ErrorCode.TIPO_INCORRECTO,
                        etiqueta + ": \"" + valor + "\" debe ser un numero con hasta 2 decimales (ej. 1500 o 1500.50).");
            }
            return valor.replace(',', '.');
        };
    }

    /** Va despues de numero() o decimal(): el valor ya es un numero valido. */
    public static Regla rango(int min, int max) {
        return (valor, etiqueta) -> {
            BigDecimal n = new BigDecimal(valor);
            if (n.compareTo(BigDecimal.valueOf(min)) < 0 || n.compareTo(BigDecimal.valueOf(max)) > 0) {
                throw new UnexoException(ErrorCode.VALOR_NO_PERMITIDO,
                        etiqueta + ": \"" + valor + "\" debe estar entre " + min + " y " + max + ".");
            }
            return valor;
        };
    }

    /** Acepta AAAA-MM-DD o DD/MM/AAAA y devuelve siempre AAAA-MM-DD. */
    public static Regla fecha() {
        return (valor, etiqueta) -> {
            try {
                Matcher latina = FECHA_LATINA.matcher(valor);
                if (latina.matches()) {
                    return LocalDate.of(Integer.parseInt(latina.group(3)), Integer.parseInt(latina.group(2)),
                            Integer.parseInt(latina.group(1))).toString();
                }
                if (FECHA_ISO.matcher(valor).matches()) {
                    return LocalDate.parse(valor).toString();
                }
            } catch (DateTimeException e) {
                throw new UnexoException(ErrorCode.FORMATO_INCORRECTO,
                        etiqueta + ": \"" + valor + "\" no es una fecha que exista.");
            }
            throw new UnexoException(ErrorCode.FORMATO_INCORRECTO,
                    etiqueta + ": \"" + valor + "\" debe ser una fecha AAAA-MM-DD o DD/MM/AAAA (ej. 2026-03-15).");
        };
    }

    /** Acepta H:MM o HH:MM y devuelve siempre HH:MM (8:30 -> 08:30). */
    public static Regla hora() {
        return (valor, etiqueta) -> {
            Matcher m = HORA.matcher(valor);
            if (!m.matches()) {
                throw new UnexoException(ErrorCode.FORMATO_INCORRECTO,
                        etiqueta + ": \"" + valor + "\" debe ser una hora HH:MM (ej. 18:30).");
            }
            try {
                return LocalTime.of(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))).toString();
            } catch (DateTimeException e) {
                throw new UnexoException(ErrorCode.FORMATO_INCORRECTO,
                        etiqueta + ": \"" + valor + "\" no es una hora valida.");
            }
        };
    }

    /** El valor debe ser uno de la lista (sin importar mayusculas); devuelve la forma oficial. */
    public static Regla unoDe(String... permitidos) {
        return (valor, etiqueta) -> {
            for (String permitido : permitidos) {
                if (permitido.equalsIgnoreCase(valor)) {
                    return permitido;
                }
            }
            throw new UnexoException(ErrorCode.VALOR_NO_PERMITIDO,
                    etiqueta + ": \"" + valor + "\" no es valido. Valores permitidos: "
                            + String.join(", ", Arrays.asList(permitidos)) + ".");
        };
    }
}
