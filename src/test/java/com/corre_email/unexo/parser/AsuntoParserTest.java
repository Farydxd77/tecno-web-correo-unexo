package com.corre_email.unexo.parser;

import com.corre_email.unexo.exception.UnexoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AsuntoParserTest {

    private static ComandoParseado parsear(String asunto) {
        return AsuntoParser.parsear(PreProcesador.normalizar(asunto));
    }

    /** asunto, codigo esperado, posicion esperada (base 0; -1 = sin posicion) */
    static Stream<Arguments> erroresDeSintaxis() {
        return Stream.of(
                Arguments.of("", 101, -1),
                Arguments.of("   ", 101, -1),
                Arguments.of("RE: ", 101, -1),
                Arguments.of("[\"123\"]", 102, 0),
                Arguments.of("\"123\"", 102, 0),
                Arguments.of("123", 103, 0),
                Arguments.of("INSPER2[\"1\"]", 103, 6),
                Arguments.of("INS PER[\"1\"]", 103, 4),
                Arguments.of("AYÚDA", 103, 2),
                Arguments.of("INSPER\"123\"]", 104, 6),
                Arguments.of("INSPER(\"123\")", 104, 6),
                Arguments.of("INSPER[\"123\"", 105, 12),
                Arguments.of("INSPER[\"123\",", 105, 13),
                Arguments.of("INSPER[\"123\")", 105, 12),
                Arguments.of("INSPER[123\"]", 106, 7),
                Arguments.of("INSPER['123']", 106, 7),
                Arguments.of("INSPER[\"123\",Juan\"]", 106, 13),
                Arguments.of("INSPER[\"123]", 107, 7),
                Arguments.of("INSPER[\"123,\"Juan\"]", 107, 11),
                Arguments.of("INSPER[\"123\" \"Juan\"]", 108, 13),
                Arguments.of("INSPER[\"123\"\"Juan\"]", 108, 12),
                Arguments.of("INSPER[\"123\",,\"Juan\"]", 109, 13),
                Arguments.of("INSPER[,\"123\"]", 109, 7),
                Arguments.of("INSPER[\"123\",]", 109, 13),
                Arguments.of("INSPER[\"123\";\"Juan\"]", 110, 12),
                Arguments.of("INSPER[\"123\"|\"Juan\"]", 110, 12),
                Arguments.of("INSPER[\"123\"] hola", 111, 14),
                Arguments.of("INSPER[#\"123\"]", 112, 7),
                Arguments.of("INSPER[\"123\"x]", 112, 12),
                Arguments.of("HELP!", 112, 4));
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1}")
    @MethodSource("erroresDeSintaxis")
    void cadaErrorDeSintaxisTieneSuCodigoYPosicion(String asunto, int codigo, int posicion) {
        UnexoException e = assertThrows(UnexoException.class, () -> parsear(asunto));

        assertEquals(codigo, e.getError().codigo(), e.getMessage());
        assertEquals(posicion, e.getPosicion(), "Posicion del error en: " + asunto);
    }

    static Stream<Arguments> comandosValidos() {
        return Stream.of(
                Arguments.of("HELP", "HELP", List.of()),
                Arguments.of("help", "HELP", List.of()),
                Arguments.of("LISPER[\"*\"]", "LISPER", List.of("*")),
                Arguments.of("LISPER[]", "LISPER", List.of()),
                Arguments.of("  insper [ \"1\" , \"Juan Perez\" ]  ", "INSPER", List.of("1", "Juan Perez")),
                Arguments.of("INSPER[\"Perez, Juan\"]", "INSPER", List.of("Perez, Juan")),
                Arguments.of("INSPER[\"a\\\"b\"]", "INSPER", List.of("a\"b")),
                Arguments.of("INSPER[\"\",\"\"]", "INSPER", List.of("", "")),
                // Lo que agregan los clientes de correo se corrige solo
                Arguments.of("RE: FW: LISPER[“*”]", "LISPER", List.of("*")),
                Arguments.of("Rv:LISPER[\"*\"]", "LISPER", List.of("*")));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("comandosValidos")
    void parseaComandosBienEscritos(String asunto, String nombre, List<String> parametros) {
        ComandoParseado parseado = parsear(asunto);

        assertEquals(nombre, parseado.nombre());
        assertEquals(parametros, parseado.parametros());
    }

    @Test
    void laPosicionSeReportaSobreElTextoNormalizado() {
        UnexoException e = assertThrows(UnexoException.class, () -> parsear("RE: INSPER[\"1\" \"2\"]"));

        assertEquals("INSPER[\"1\" \"2\"]", e.getEntrada());
        assertEquals(11, e.getPosicion());
    }
}
