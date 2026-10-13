package com.corre_email.unexo.validation;

import com.corre_email.unexo.exception.ErrorCode;
import com.corre_email.unexo.exception.UnexoException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Las reglas nuevas: fecha, hora, decimal y rango. */
class ReglasTest {

    private static ErrorCode falla(Regla regla, String valor) {
        return assertThrows(UnexoException.class, () -> regla.aplicar(valor, "Parametro 1 (x)")).getError();
    }

    @Test
    void fechaAceptaDosFormatosYDevuelveSiempreIso() {
        assertEquals("2026-03-15", Reglas.fecha().aplicar("2026-03-15", "x"));
        assertEquals("2026-03-05", Reglas.fecha().aplicar("5/3/2026", "x"));
    }

    @Test
    void fechaMalEscritaOInexistente305() {
        assertEquals(ErrorCode.FORMATO_INCORRECTO, falla(Reglas.fecha(), "15-03-2026"));
        assertEquals(ErrorCode.FORMATO_INCORRECTO, falla(Reglas.fecha(), "2026-02-30"));
        assertEquals(ErrorCode.FORMATO_INCORRECTO, falla(Reglas.fecha(), "30/02/2026"));
    }

    @Test
    void horaSeCompletaConCero() {
        assertEquals("08:30", Reglas.hora().aplicar("8:30", "x"));
        assertEquals(ErrorCode.FORMATO_INCORRECTO, falla(Reglas.hora(), "25:00"));
        assertEquals(ErrorCode.FORMATO_INCORRECTO, falla(Reglas.hora(), "8.30"));
    }

    @Test
    void decimalAceptaComaYPunto() {
        assertEquals("1500.50", Reglas.decimal().aplicar("1500,50", "x"));
        assertEquals("1500", Reglas.decimal().aplicar("1500", "x"));
        assertEquals(ErrorCode.TIPO_INCORRECTO, falla(Reglas.decimal(), "1.500,00"));
        assertEquals(ErrorCode.TIPO_INCORRECTO, falla(Reglas.decimal(), "10.123"));
    }

    @Test
    void rangoIncluyeLosExtremos() {
        assertEquals("0", Reglas.rango(0, 100).aplicar("0", "x"));
        assertEquals("100", Reglas.rango(0, 100).aplicar("100", "x"));
        assertEquals(ErrorCode.VALOR_NO_PERMITIDO, falla(Reglas.rango(0, 100), "100.01"));
    }
}
