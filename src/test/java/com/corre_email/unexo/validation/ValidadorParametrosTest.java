package com.corre_email.unexo.validation;

import com.corre_email.unexo.command.PersonaComandos;
import com.corre_email.unexo.exception.ErrorCode;
import com.corre_email.unexo.exception.UnexoException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidadorParametrosTest {

    private final DefinicionComando insper = new PersonaComandos(null, null).insper().definicion();
    private final DefinicionComando lisper = new PersonaComandos(null, null).lisper().definicion();

    private static List<String> persona(String ci, String nombre, String apellido, String tipo,
            String telefono, String celular, String email) {
        return List.of(ci, nombre, apellido, tipo, telefono, celular, email);
    }

    private static List<String> personaValida() {
        return persona("12345678", "Juan", "Perez", "Docente", "3312345", "70011223", "juan@tecnoweb.org.bo");
    }

    private UnexoException fallaInsper(List<String> valores) {
        return assertThrows(UnexoException.class, () -> ValidadorParametros.validar(insper, valores));
    }

    @Test
    void aceptaUnaPersonaValidaYNormalizaElTipo() {
        Parametros p = ValidadorParametros.validar(insper,
                persona(" 12345678 ", "José María", "Pérez-Soto", "docente", "", "70011223", "juan@tecnoweb.org.bo"));

        assertEquals("12345678", p.get("ci"));
        assertEquals("Docente", p.get("rol"));
        assertNull(p.opcional("telefono"));
    }

    @Test
    void faltanParametros301() {
        UnexoException e = fallaInsper(List.of("1", "2", "3", "4", "5"));

        assertEquals(ErrorCode.FALTAN_PARAMETROS, e.getError());
        assertTrue(e.getMessage().contains("se recibieron 5 (faltan 2)"), e.getMessage());
        assertTrue(e.getAyuda().contains("Formato correcto: INSPER["), e.getAyuda());
    }

    @Test
    void sobranParametros302() {
        UnexoException e = fallaInsper(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9"));

        assertEquals(ErrorCode.SOBRAN_PARAMETROS, e.getError());
    }

    @Test
    void obligatorioVacio303() {
        UnexoException e = fallaInsper(persona("12345678", " ", "Perez", "Docente", "", "70011223", "a@b.com"));

        assertEquals(ErrorCode.PARAMETRO_VACIO, e.getError());
        assertTrue(e.getMessage().startsWith("Parametro 2 (nombre)"), e.getMessage());
    }

    @Test
    void tipoIncorrecto304() {
        assertEquals(ErrorCode.TIPO_INCORRECTO,
                fallaInsper(persona("12.345.678", "Juan", "Perez", "Docente", "", "70011223", "a@b.com")).getError());
        assertEquals(ErrorCode.TIPO_INCORRECTO,
                fallaInsper(persona("12345678", "Juan3", "Perez", "Docente", "", "70011223", "a@b.com")).getError());
    }

    @Test
    void formatoIncorrecto305ExplicaQueFalta() {
        UnexoException sinArroba = fallaInsper(persona("12345678", "Juan", "Perez", "Docente", "", "70011223", "juan.tecnoweb"));
        UnexoException sinDominio = fallaInsper(persona("12345678", "Juan", "Perez", "Docente", "", "70011223", "juan@"));

        assertEquals(ErrorCode.FORMATO_INCORRECTO, sinArroba.getError());
        assertTrue(sinArroba.getMessage().contains("le falta la @"), sinArroba.getMessage());
        assertTrue(sinDominio.getMessage().contains("le falta el dominio"), sinDominio.getMessage());
    }

    @Test
    void largoFueraDeRango306() {
        UnexoException e = fallaInsper(persona("12345678", "Juan", "Perez", "Docente", "", "123", "a@b.com"));

        assertEquals(ErrorCode.LARGO_FUERA_DE_RANGO, e.getError());
        assertTrue(e.getMessage().contains("exactamente 8 caracteres y tiene 3"), e.getMessage());
    }

    @Test
    void valorNoPermitido307() {
        UnexoException e = fallaInsper(persona("12345678", "Juan", "Perez", "Profe", "", "70011223", "a@b.com"));

        assertEquals(ErrorCode.VALOR_NO_PERMITIDO, e.getError());
        assertTrue(e.getMessage().contains("Administrador, Docente, Estudiante"), e.getMessage());
    }

    @Test
    void comodinNoPermitido308() {
        UnexoException e = fallaInsper(persona("*", "Juan", "Perez", "Docente", "", "70011223", "a@b.com"));

        assertEquals(ErrorCode.COMODIN_NO_PERMITIDO, e.getError());
    }

    @Test
    void variosErrores300SeInformanTodosJuntos() {
        UnexoException e = fallaInsper(persona("12A45", "Juan", "Perez", "Profe", "", "70011223", "juan@"));

        assertEquals(ErrorCode.ERRORES_EN_PARAMETROS, e.getError());
        assertEquals(List.of(ErrorCode.TIPO_INCORRECTO, ErrorCode.VALOR_NO_PERMITIDO, ErrorCode.FORMATO_INCORRECTO),
                e.getErrores().stream().map(UnexoException::getError).toList());
    }

    @Test
    void lisperAceptaNadaOAsterisco() {
        ValidadorParametros.validar(lisper, List.of());
        assertTrue(ValidadorParametros.validar(lisper, List.of("*")).esComodin("filtro"));

        assertEquals(ErrorCode.VALOR_NO_PERMITIDO,
                assertThrows(UnexoException.class, () -> ValidadorParametros.validar(lisper, List.of("Juan"))).getError());
        assertEquals(ErrorCode.SOBRAN_PARAMETROS,
                assertThrows(UnexoException.class, () -> ValidadorParametros.validar(lisper, List.of("*", "*"))).getError());
    }

    @Test
    void laPersonaValidaDeReferenciaPasa() {
        ValidadorParametros.validar(insper, personaValida());
    }
}
