package com.corre_email.unexo.validation;

import com.corre_email.unexo.exception.ErrorCode;
import com.corre_email.unexo.exception.UnexoException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Etapa 4: aplica la definicion del comando a los valores recibidos.
 * <p>
 * Si la cantidad no cuadra se corta ahi (301/302): al ser posicionales, con uno
 * de menos todos los siguientes quedan corridos y validarlos solo confundiria.
 * Si la cantidad esta bien, se revisan TODOS los parametros y se informan todos
 * los errores juntos (300), porque cada intento le cuesta un correo al usuario.
 */
public final class ValidadorParametros {

    private ValidadorParametros() {
    }

    public static Parametros validar(DefinicionComando definicion, List<String> valores) {
        List<ParamDef> declarados = definicion.parametros();
        int recibidos = valores.size();
        int minimo = definicion.minimoParametros();
        int maximo = definicion.maximoParametros();

        if (recibidos < minimo || recibidos > maximo) {
            throw errorDeCantidad(definicion, recibidos, minimo, maximo);
        }

        Parametros resultado = new Parametros();
        List<UnexoException> errores = new ArrayList<>();

        for (int i = 0; i < declarados.size(); i++) {
            ParamDef param = declarados.get(i);
            String valor = i < recibidos ? valores.get(i).trim() : "";
            String etiqueta = "Parametro " + (i + 1) + " (" + param.nombre() + ")";
            try {
                resultado.poner(param.nombre(), validarUno(param, valor, etiqueta));
            } catch (UnexoException e) {
                errores.add(e);
            }
        }

        if (errores.size() == 1) {
            throw errores.getFirst().conAyuda(definicion.ayuda());
        }
        if (errores.size() > 1) {
            throw new UnexoException(ErrorCode.ERRORES_EN_PARAMETROS, "Corrija los siguientes parametros:")
                    .conErrores(errores)
                    .conAyuda(definicion.ayuda());
        }
        return resultado;
    }

    private static String validarUno(ParamDef param, String valor, String etiqueta) {
        if (valor.isEmpty()) {
            if (param.obligatorio()) {
                throw new UnexoException(ErrorCode.PARAMETRO_VACIO, etiqueta + ": es obligatorio y llego vacio.");
            }
            return "";
        }
        if (valor.equals("*")) {
            if (param.aceptaComodin()) {
                return valor;
            }
            throw new UnexoException(ErrorCode.COMODIN_NO_PERMITIDO,
                    etiqueta + ": no acepta \"*\". Escriba el valor real.");
        }
        // Cada regla recibe el valor ya normalizado por la anterior; se corta en la primera que falla
        for (Regla regla : param.reglas()) {
            valor = regla.aplicar(valor, etiqueta);
        }
        return valor;
    }

    private static UnexoException errorDeCantidad(DefinicionComando definicion, int recibidos, int minimo, int maximo) {
        String esperados = minimo == maximo
                ? String.valueOf(maximo)
                : "entre " + minimo + " y " + maximo;
        String nombres = definicion.parametros().stream()
                .map(ParamDef::nombre)
                .collect(Collectors.joining(", "));

        if (recibidos < minimo) {
            int faltan = minimo - recibidos;
            return new UnexoException(ErrorCode.FALTAN_PARAMETROS,
                    definicion.nombre() + " necesita " + esperados + " parametros y se recibieron " + recibidos
                            + " (falta" + (faltan == 1 ? " 1" : "n " + faltan) + ").\n"
                            + "Orden esperado: " + nombres + ".\n"
                            + "Si un dato opcional no aplica, escribalo vacio: \"\"")
                    .conAyuda(definicion.ayuda());
        }

        int sobran = recibidos - maximo;
        String detalle = maximo == 0
                ? definicion.nombre() + " no lleva parametros y se recibieron " + recibidos + "."
                : definicion.nombre() + " acepta " + esperados + " parametros y se recibieron " + recibidos
                        + " (sobra" + (sobran == 1 ? " 1" : "n " + sobran) + ").\n"
                        + "Orden esperado: " + nombres + ".";
        return new UnexoException(ErrorCode.SOBRAN_PARAMETROS, detalle).conAyuda(definicion.ayuda());
    }
}
