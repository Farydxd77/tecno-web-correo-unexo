package com.corre_email.unexo.service;

import com.corre_email.unexo.exception.ErrorCode;
import com.corre_email.unexo.exception.UnexoException;
import org.springframework.mail.MailSendException;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Todo lo que el sistema escribe en consola pasa por aqui, siempre con el mismo
 * formato, para que cualquiera lo pueda leer:
 *
 * <pre>
 * +-- CORREO 12:51:02 ---------------------------------------------------
 * | De       : ana@tecnoweb.org.bo
 * | Asunto   : INSMDL["JAVA-1",...]
 * | Resultado: OK
 * | Envio    : OK, respuesta enviada
 * | Buzon    : se borra
 * +---------------------------------------------------------------------
 * </pre>
 *
 * Solo caracteres ASCII: la consola de Windows muestra "?" en lugar de emojis.
 */
public final class Bitacora {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int ANCHO = 70;
    private static final String CONTINUACION = "|            ";

    private Bitacora() {
    }

    /**
     * Un bloque se arma entero y se imprime de una vez al final, asi sus lineas
     * nunca quedan mezcladas con otros mensajes.
     */
    public static final class Bloque {

        private final StringBuilder sb = new StringBuilder();
        private Throwable traza;

        private Bloque(String titulo) {
            String cabeza = "+-- " + titulo + " " + LocalTime.now().format(HORA) + " ";
            sb.append(cabeza).append("-".repeat(Math.max(3, ANCHO - cabeza.length()))).append('\n');
        }

        /** Una fila "Etiqueta : texto"; si el texto tiene varias lineas, las siguientes van alineadas. */
        public Bloque fila(String etiqueta, String texto) {
            String[] lineas = (texto == null || texto.isBlank() ? "-" : texto.strip()).split("\n");
            for (int i = 0; i < lineas.length; i++) {
                sb.append(i == 0 && !etiqueta.isEmpty() ? String.format("| %-9s: ", etiqueta) : CONTINUACION)
                        .append(lineas[i].strip()).append('\n');
            }
            return this;
        }

        /** Una linea divisoria con titulo, para separar secciones dentro del bloque. */
        public Bloque seccion(String titulo) {
            String cabeza = "|-- " + titulo + " ";
            sb.append(cabeza).append("-".repeat(Math.max(3, ANCHO - cabeza.length()))).append('\n');
            return this;
        }

        /** El codigo, el titulo, el mensaje, los errores de cada parametro y la causa tecnica. */
        public Bloque error(String etiqueta, UnexoException e) {
            fila(etiqueta, "ERROR " + e.getError().codigo() + " - " + e.getError().titulo());
            if (e.getMessage() != null) {
                fila("", e.getMessage());
            }
            for (UnexoException sub : e.getErrores()) {
                fila("", "- [" + sub.getError().codigo() + "] " + sub.getMessage());
            }
            if (e.getCause() != null) {
                fila("Detalle", causaReal(e.getCause()));
                // Lo inesperado se imprime con la traza completa (debajo del bloque) para poder corregirlo
                if (e.getError() == ErrorCode.ERROR_INTERNO || e.getError() == ErrorCode.BD_ERROR_SQL) {
                    traza = e.getCause();
                }
            }
            return this;
        }

        public void imprimir() {
            sb.append('+').append("-".repeat(ANCHO - 1)).append('\n');
            System.out.print(sb);
            if (traza != null) {
                System.out.println("Traza tecnica del error anterior (para el programador):");
                traza.printStackTrace(System.out);
            }
        }
    }

    /** Bloque de un correo del buzon. */
    public static Bloque correo(String de, String asunto) {
        return new Bloque("CORREO").fila("De", de).fila("Asunto", asunto);
    }

    /** Bloque de un problema del sistema que no es de un correo (servidor caido, BD caida al arrancar...). */
    public static Bloque sistema() {
        return new Bloque("SISTEMA");
    }

    /** Una sola linea con la hora, para avisos cortos. */
    public static void linea(String texto) {
        System.out.println("[" + LocalTime.now().format(HORA) + "] " + texto);
    }

    /**
     * La causa de fondo en una linea. Los errores suelen venir envueltos en otros
     * (ej. Spring -> JavaMail -> SMTP); el mensaje util es el del ultimo.
     */
    static String causaReal(Throwable e) {
        Throwable t = e;
        // Spring guarda el rechazo del servidor aparte, no como causa
        if (t instanceof MailSendException mse && !mse.getFailedMessages().isEmpty()) {
            t = mse.getFailedMessages().values().iterator().next();
        }
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        String mensaje = t.getMessage() == null ? "" : t.getMessage().lines().findFirst().orElse("").strip();
        return mensaje.isEmpty() ? t.getClass().getSimpleName() : mensaje;
    }
}
