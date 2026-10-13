package com.corre_email.unexo.parser;

import com.corre_email.unexo.exception.ErrorCode;
import com.corre_email.unexo.exception.UnexoException;

import java.util.ArrayList;
import java.util.List;

/**
 * Etapa 1: lee el asunto caracter por caracter y, si algo esta mal escrito,
 * dice exactamente que y en que posicion (errores 1xx). Se detiene en el
 * primer error, como un compilador: despues de una coma faltante el resto ya
 * no se puede interpretar con seguridad.
 *
 * <pre>
 * asunto  := NOMBRE ( '[' lista? ']' )?
 * lista   := valor ( ',' valor )*
 * valor   := '"' texto '"'        (dentro del texto: \" y \\ para escapar)
 * </pre>
 */
public final class AsuntoParser {

    private final String texto;
    private int pos;

    private AsuntoParser(String texto) {
        this.texto = texto;
    }

    /** @param asunto el asunto ya pasado por {@link PreProcesador#normalizar(String)} */
    public static ComandoParseado parsear(String asunto) {
        if (asunto == null || asunto.isBlank()) {
            throw new UnexoException(ErrorCode.ASUNTO_VACIO,
                    "El asunto del correo esta vacio. Escriba el comando en el ASUNTO, por ejemplo: HELP");
        }
        return new AsuntoParser(asunto).comando();
    }

    private ComandoParseado comando() {
        saltarEspacios();
        String nombre = nombre();
        saltarEspacios();

        if (fin()) {
            return new ComandoParseado(nombre, List.of());
        }

        char c = actual();
        if (c == '[') {
            pos++;
            List<String> parametros = lista();
            saltarEspacios();
            if (!fin()) {
                throw error(ErrorCode.TEXTO_DESPUES_DEL_CIERRE,
                        "El comando debe terminar en ']' y despues hay mas texto.");
            }
            return new ComandoParseado(nombre, parametros);
        }
        if (c == '"') {
            throw error(ErrorCode.FALTA_CORCHETE_APERTURA,
                    "Falta '[' despues de " + nombre + ". Los parametros van entre corchetes: " + nombre + "[\"...\"]");
        }
        if (c == '(' || c == '{') {
            throw error(ErrorCode.FALTA_CORCHETE_APERTURA,
                    "Use corchetes [ ] en lugar de '" + c + "'.");
        }
        if (esLetra(c)) {
            throw error(ErrorCode.NOMBRE_COMANDO_INVALIDO,
                    "El asunto debe tener solo el comando, sin otras palabras. Despues de " + nombre
                            + " no puede ir un espacio con mas texto. Ejemplo: LISPER[\"*\"]");
        }
        throw error(ErrorCode.CARACTER_INESPERADO,
                "Se esperaba '[' despues de " + nombre + " y se encontro '" + c + "'.");
    }

    private String nombre() {
        char c = actual();
        if (c == '[' || c == '"') {
            throw error(ErrorCode.FALTA_NOMBRE_COMANDO,
                    "El asunto debe empezar con el nombre del comando, por ejemplo: LISPER[\"*\"]");
        }
        if (!esLetra(c)) {
            throw error(ErrorCode.NOMBRE_COMANDO_INVALIDO,
                    "El asunto debe empezar con el nombre de un comando (por ejemplo HELP) y empieza con '"
                            + c + "'.");
        }

        int inicio = pos;
        while (!fin() && esLetra(actual())) {
            pos++;
        }
        if (!fin() && (Character.isLetterOrDigit(actual()) || actual() == '_' || actual() == '-')) {
            throw error(ErrorCode.NOMBRE_COMANDO_INVALIDO,
                    "El nombre del comando solo puede tener letras (sin numeros, tildes ni simbolos) y se encontro '"
                            + actual() + "'.");
        }
        return texto.substring(inicio, pos).toUpperCase();
    }

    /** Se llama justo despues del '['. Consume hasta el ']' incluido. */
    private List<String> lista() {
        List<String> valores = new ArrayList<>();
        saltarEspacios();
        if (!fin() && actual() == ']') {
            pos++;
            return valores;
        }

        while (true) {
            // ---- se espera un valor ----
            saltarEspacios();
            if (fin()) {
                throw error(ErrorCode.FALTA_CORCHETE_CIERRE, "Falta ']' al final del comando.");
            }
            char c = actual();
            int inicioValor = pos;
            if (c == '"') {
                valores.add(valor());
            } else if (c == ',') {
                throw error(ErrorCode.COMA_SOBRANTE, valores.isEmpty()
                        ? "Hay una coma antes del primer parametro."
                        : "Hay dos comas seguidas. Si un dato va vacio escribalo como \"\".");
            } else if (c == ']') {
                throw error(ErrorCode.COMA_SOBRANTE, "Hay una coma de mas antes de ']'.");
            } else if (c == '\'') {
                throw error(ErrorCode.FALTA_COMILLA_APERTURA,
                        "Use comillas dobles \" en lugar de comillas simples '.");
            } else if (Character.isLetterOrDigit(c) || c == '*' || c == '@' || c == '.' || c == '-') {
                throw error(ErrorCode.FALTA_COMILLA_APERTURA,
                        "El parametro " + (valores.size() + 1) + " debe ir entre comillas dobles, asi: \""
                                + palabraDesde(pos) + "\"");
            } else {
                throw error(ErrorCode.CARACTER_INESPERADO,
                        "Se esperaba un parametro entre comillas y se encontro '" + c + "'.");
            }

            // ---- despues de un valor se espera ',' o ']' ----
            saltarEspacios();
            if (fin()) {
                throw error(ErrorCode.FALTA_CORCHETE_CIERRE, "Falta ']' al final del comando.");
            }
            c = actual();
            String ultimo = valores.getLast();
            if (c == ',') {
                pos++;
                continue;
            }
            if (c == ']') {
                pos++;
                return valores;
            }
            if (c == '"') {
                throw error(ErrorCode.FALTA_COMA, "Se esperaba una coma ',' despues de \"" + ultimo + "\".");
            }
            if (c == ';' || c == '|' || c == ':' || c == '.' || c == '/') {
                throw error(ErrorCode.SEPARADOR_INCORRECTO,
                        "Los parametros se separan con coma ',' y no con '" + c + "'.");
            }
            if (c == ')' || c == '}') {
                throw error(ErrorCode.FALTA_CORCHETE_CIERRE, "Use ']' para cerrar la lista, no '" + c + "'.");
            }

            // Letra pegada a un valor ya cerrado, ej: "123,"Juan". Lo tipico es que
            // falte la comilla de cierre ANTES de una coma del valor anterior.
            int comaInterna = texto.indexOf(',', inicioValor + 1);
            if (comaInterna >= 0 && comaInterna < pos && ultimo.contains(",")) {
                throw error(ErrorCode.FALTA_COMILLA_CIERRE,
                        "Parece que falta cerrar la comilla del parametro " + valores.size()
                                + " antes de esta coma.",
                        comaInterna);
            }
            throw error(ErrorCode.CARACTER_INESPERADO,
                    "Se esperaba ',' o ']' despues de \"" + ultimo + "\" y se encontro '" + c
                            + "'. Revise si falta o sobra una comilla.");
        }
    }

    /** Se llama parado en la comilla de apertura. Deja pos despues de la comilla de cierre. */
    private String valor() {
        int apertura = pos;
        pos++;
        StringBuilder sb = new StringBuilder();
        while (!fin()) {
            char c = actual();
            if (c == '\\' && pos + 1 < texto.length()
                    && (texto.charAt(pos + 1) == '"' || texto.charAt(pos + 1) == '\\')) {
                sb.append(texto.charAt(pos + 1));
                pos += 2;
                continue;
            }
            if (c == '"') {
                pos++;
                return sb.toString();
            }
            sb.append(c);
            pos++;
        }
        throw error(ErrorCode.FALTA_COMILLA_CIERRE, "La comilla que abre aqui nunca se cierra.", apertura);
    }

    // ---------- Apoyo ----------

    private boolean fin() {
        return pos >= texto.length();
    }

    private char actual() {
        return texto.charAt(pos);
    }

    private void saltarEspacios() {
        while (!fin() && Character.isWhitespace(actual())) {
            pos++;
        }
    }

    private static boolean esLetra(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    /** La palabra que empieza en {@code desde}, para sugerir como deberia escribirse. */
    private String palabraDesde(int desde) {
        int hasta = desde;
        while (hasta < texto.length() && ",]\"".indexOf(texto.charAt(hasta)) < 0) {
            hasta++;
        }
        return texto.substring(desde, hasta).trim();
    }

    private UnexoException error(ErrorCode codigo, String detalle) {
        return error(codigo, detalle, pos);
    }

    private UnexoException error(ErrorCode codigo, String detalle, int posicion) {
        return new UnexoException(codigo, detalle).enPosicion(texto, posicion);
    }
}
