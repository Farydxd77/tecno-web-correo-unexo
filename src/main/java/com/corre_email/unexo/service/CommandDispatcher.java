package com.corre_email.unexo.service;

import com.corre_email.unexo.command.CommandStrategy;
import com.corre_email.unexo.exception.ErrorCode;
import com.corre_email.unexo.exception.TraductorExcepciones;
import com.corre_email.unexo.exception.UnexoException;
import com.corre_email.unexo.parser.AsuntoParser;
import com.corre_email.unexo.parser.ComandoParseado;
import com.corre_email.unexo.parser.PreProcesador;
import com.corre_email.unexo.validation.Parametros;
import com.corre_email.unexo.validation.ValidadorParametros;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Orquesta las etapas por las que pasa cada asunto:
 * pre-proceso -> parser -> comando -> parametros -> ejecucion.
 * Cualquier fallo sale como UnexoException con su codigo.
 */
@Service
public class CommandDispatcher {

    private final List<CommandStrategy> strategies;

    public CommandDispatcher(List<CommandStrategy> strategies) {
        this.strategies = strategies;
    }

    /**
     * @return el texto de respuesta si el comando se ejecuto bien
     * @throws UnexoException en cualquier error, ya clasificado
     */
    public String ejecutar(String asunto) {
        try {
            String texto = PreProcesador.normalizar(asunto);
            ComandoParseado parseado = AsuntoParser.parsear(texto);
            CommandStrategy comando = buscar(parseado.nombre());
            // Etapa 3 (permisos del remitente): pendiente hasta tener roles cargados
            Parametros params = ValidadorParametros.validar(comando.definicion(), parseado.parametros());
            return comando.execute(params);
        } catch (RuntimeException e) {
            throw TraductorExcepciones.traducir(e);
        }
    }

    private CommandStrategy buscar(String nombre) {
        return strategies.stream()
                .filter(s -> s.definicion().responde(nombre))
                .findFirst()
                .orElseThrow(() -> comandoNoExiste(nombre));
    }

    private UnexoException comandoNoExiste(String nombre) {
        List<String> conocidos = strategies.stream()
                .flatMap(s -> Stream.concat(Stream.of(s.definicion().nombre()), s.definicion().alias().stream()))
                .toList();

        // Se tolera 1 letra mal cada 3 (minimo 1): INSPR -> INSPER si, HOLA -> HELP no
        String sugerencia = null;
        int mejor = Integer.MAX_VALUE;
        for (String conocido : conocidos) {
            int d = distancia(nombre, conocido);
            if (d <= Math.max(1, conocido.length() / 3) && d < mejor) {
                mejor = d;
                sugerencia = conocido;
            }
        }

        String detalle = "El comando " + nombre + " no existe.";
        if (sugerencia != null) {
            detalle += " ¿Quiso decir " + sugerencia + "?";
        }
        String disponibles = strategies.stream()
                .map(s -> s.definicion().nombre())
                .sorted()
                .collect(Collectors.joining(", "));
        return new UnexoException(ErrorCode.COMANDO_NO_EXISTE, detalle)
                .conAyuda("Comandos disponibles: " + disponibles);
    }

    /** Distancia de Levenshtein: cuantas letras hay que cambiar, agregar o quitar para pasar de a a b. */
    private static int distancia(String a, String b) {
        int[] anterior = new int[b.length() + 1];
        int[] actual = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            anterior[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            actual[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int costo = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                actual[j] = Math.min(Math.min(actual[j - 1] + 1, anterior[j] + 1), anterior[j - 1] + costo);
            }
            int[] tmp = anterior;
            anterior = actual;
            actual = tmp;
        }
        return anterior[b.length()];
    }
}
