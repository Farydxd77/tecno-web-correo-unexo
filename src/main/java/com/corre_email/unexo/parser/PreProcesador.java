package com.corre_email.unexo.parser;

import jakarta.mail.internet.MimeUtility;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Etapa 0: corrige en silencio lo que no es culpa del usuario, antes de parsear.
 * Los celulares y Outlook cambian solos " por comillas curvas, y al responder
 * un correo el cliente agrega "RE:" al asunto.
 */
public final class PreProcesador {

    private static final Pattern PREFIJOS = Pattern.compile(
            "^\\s*((re|rv|res|fw|fwd|reenv|tr|aw)\\s*:\\s*)+", Pattern.CASE_INSENSITIVE);

    /**
     * Texto "comprimido" segun RFC 2047, ej. =?UTF-8?Q?=C2=A1hola?= o =?UTF-8?B?wqFob2xh?=.
     * Asi viaja un asunto con tildes, ñ o comillas curvas.
     */
    private static final Pattern PALABRA_CODIFICADA = Pattern.compile("=\\?[^?\\s]+\\?[BbQq]\\?[^?\\s]*\\?=");
    /** Entre dos palabras codificadas seguidas el espacio no cuenta (RFC 2047). */
    private static final Pattern ESPACIO_ENTRE_CODIFICADAS = Pattern.compile("(?<=\\?=)\\s+(?==\\?)");

    private PreProcesador() {
    }

    public static String normalizar(String asunto) {
        if (asunto == null) {
            return "";
        }
        String texto = decodificar(asunto)
                .replaceAll("[\\u200B\\u200C\\u200D\\uFEFF\\u00AD]", "")
                .replaceAll("[\\r\\n\\t\\u00A0]", " ")
                .replaceAll("[\\u201C\\u201D\\u201E\\u201F\\u2033\\u00AB\\u00BB\\uFF02]", "\"");
        return PREFIJOS.matcher(texto).replaceFirst("").trim();
    }

    /**
     * JavaMail ya decodifica el asunto normal, pero NO cuando la parte codificada
     * viene pegada a otro texto (ej. "RE:=?UTF-8?Q?=C2=A1hola?= mundo"). En ese
     * caso el usuario veria el texto "comprimido" en la respuesta, asi que se
     * decodifica aqui cada parte que haya quedado sin decodificar.
     */
    static String decodificar(String asunto) {
        if (!asunto.contains("=?")) {
            return asunto;
        }
        Matcher m = PALABRA_CODIFICADA.matcher(ESPACIO_ENTRE_CODIFICADAS.matcher(asunto).replaceAll(""));
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String decodificada;
            try {
                decodificada = MimeUtility.decodeWord(m.group());
            } catch (Exception e) {
                // Charset desconocido o mal formado: se deja tal cual y el parser dira que esta mal
                decodificada = m.group();
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(decodificada));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
