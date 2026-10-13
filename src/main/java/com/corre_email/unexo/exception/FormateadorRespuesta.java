package com.corre_email.unexo.exception;

/** Arma el texto del correo que recibe el usuario cuando su comando falla. */
public final class FormateadorRespuesta {

    private static final int CONTEXTO = 12;
    private static final String PIE = "Envie un correo con asunto HELP para ver todos los comandos.";

    private FormateadorRespuesta() {
    }

    public static String formatear(UnexoException e) {
        ErrorCode error = e.getError();
        StringBuilder sb = new StringBuilder();

        String titulo = error == ErrorCode.ERRORES_EN_PARAMETROS
                ? "HAY " + e.getErrores().size() + " PARAMETROS CON ERRORES"
                : error.titulo();
        sb.append("ERROR ").append(error.codigo()).append(" - ").append(titulo).append("\n\n");

        if (e.tienePosicion()) {
            sb.append("Su comando:\n");
            sb.append("  ").append(e.getEntrada()).append('\n');
            sb.append("  ").append(" ".repeat(e.getPosicion())).append("^\n");
        }

        if (e.getMessage() != null) {
            sb.append(e.getMessage()).append('\n');
        }

        if (e.tienePosicion()) {
            sb.append("Posicion: ").append(e.getPosicion() + 1)
                    .append(" | Cerca de: ").append(cercaDe(e.getEntrada(), e.getPosicion())).append('\n');
        }

        for (UnexoException sub : e.getErrores()) {
            sb.append("- [").append(sub.getError().codigo()).append("] ").append(sub.getMessage()).append('\n');
        }

        if (e.getAyuda() != null) {
            sb.append('\n').append(e.getAyuda()).append('\n');
        }

        sb.append('\n').append(PIE);
        return sb.toString();
    }

    /**
     * Fragmento alrededor del error. Se agrega ademas de la flecha ^ porque muchos
     * clientes de correo usan fuentes proporcionales y la flecha puede quedar corrida.
     */
    private static String cercaDe(String texto, int pos) {
        int desde = Math.max(0, pos - CONTEXTO);
        int hasta = Math.min(texto.length(), pos + CONTEXTO + 1);
        return (desde > 0 ? "..." : "") + texto.substring(desde, hasta) + (hasta < texto.length() ? "..." : "");
    }
}
