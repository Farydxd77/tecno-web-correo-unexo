package com.corre_email.unexo.service;

import com.corre_email.unexo.command.CommandStrategy;
import com.corre_email.unexo.command.HelpCommand;
import com.corre_email.unexo.command.PersonaComandos;
import com.corre_email.unexo.exception.Accion;
import com.corre_email.unexo.exception.ErrorCode;
import com.corre_email.unexo.exception.UnexoException;
import com.corre_email.unexo.model.Persona;
import com.corre_email.unexo.model.Role;
import com.corre_email.unexo.repository.PersonaRepository;
import com.corre_email.unexo.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.transaction.CannotCreateTransactionException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommandDispatcherTest {

    private static final String INSPER_VALIDO =
            "INSPER[\"12345678\",\"Juan\",\"Perez\",\"Docente\",\"3312345\",\"70011223\",\"juan@tecnoweb.org.bo\"]";

    private PersonaRepository repo;
    private CommandDispatcher dispatcher;

    @BeforeEach
    void armarSistema() {
        repo = mock(PersonaRepository.class);
        List<CommandStrategy> comandos = new ArrayList<>();
        comandos.add(new HelpCommand(() -> comandos));
        RoleRepository roles = mock(RoleRepository.class);
        when(roles.buscarOCrear(any())).thenAnswer(inv -> new Role(inv.getArgument(0)));
        PersonaComandos persona = new PersonaComandos(repo, roles);
        comandos.addAll(List.of(persona.lisper(), persona.verper(), persona.insper(), persona.modper(), persona.eliper()));
        dispatcher = new CommandDispatcher(comandos);
    }

    private UnexoException falla(String asunto) {
        return assertThrows(UnexoException.class, () -> dispatcher.ejecutar(asunto));
    }

    // ---------- HELP ----------

    @Test
    void helpSeArmaConLasDefinicionesDeLosComandos() {
        for (String asunto : List.of("HELP", "help", "AYUDA", "RE: ayuda", "HELP[]")) {
            String respuesta = dispatcher.ejecutar(asunto);
            assertTrue(respuesta.contains("COMANDOS DISPONIBLES"), asunto);
        }

        String ayuda = dispatcher.ejecutar("HELP");
        assertTrue(ayuda.contains("INSPER[\"ci\",\"nombre\",\"apellido\",\"rol\",\"telefono\",\"celular\",\"email\",\"correo_institucional\"]"), ayuda);
        assertTrue(ayuda.contains("LISPER[\"*\"]"), ayuda);
        assertTrue(ayuda.contains("(tambien: AYUDA)"), ayuda);
        assertTrue(ayuda.contains("Opcionales (pueden ir vacios \"\"): telefono"), ayuda);
        assertTrue(ayuda.indexOf("HELP") < ayuda.indexOf("INSPER"), "HELP va primero");
    }

    @Test
    void helpNoLlevaParametros() {
        assertEquals(ErrorCode.SOBRAN_PARAMETROS, falla("HELP[\"x\"]").getError());
    }

    // ---------- 2xx ----------

    @Test
    void comandoInexistente201SugiereElParecido() {
        UnexoException e = falla("INSPR[\"1\"]");

        assertEquals(ErrorCode.COMANDO_NO_EXISTE, e.getError());
        assertTrue(e.getMessage().contains("¿Quiso decir INSPER?"), e.getMessage());
        assertEquals("Comandos disponibles: ELIPER, HELP, INSPER, LISPER, MODPER, VERPER", e.getAyuda());
    }

    @Test
    void comandoInexistente201SinSugerenciaSiNoSeParece() {
        UnexoException e = falla("HOLA");

        assertEquals(ErrorCode.COMANDO_NO_EXISTE, e.getError());
        assertFalse(e.getMessage().contains("Quiso decir"), e.getMessage());
    }

    // ---------- INSPER y 4xx ----------

    @Test
    void insperValidoGuardaLaPersona() {
        String respuesta = dispatcher.ejecutar(INSPER_VALIDO);

        assertTrue(respuesta.startsWith("OK - Persona registrada correctamente."), respuesta);
        verify(repo).save(any(Persona.class));
    }

    @Test
    void ciDuplicado401NoSobrescribe() {
        when(repo.existsById("12345678")).thenReturn(true);

        assertEquals(ErrorCode.REGISTRO_DUPLICADO, falla(INSPER_VALIDO).getError());
        verify(repo, never()).save(any());
    }

    @Test
    void emailRepetido403() {
        when(repo.existsByEmailIgnoreCase("juan@tecnoweb.org.bo")).thenReturn(true);

        assertEquals(ErrorCode.DATO_UNICO_REPETIDO, falla(INSPER_VALIDO).getError());
        verify(repo, never()).save(any());
    }

    // ---------- 5xx: los errores de Spring se traducen ----------

    @Test
    void bdSinConexion501SeLeAvisaAlUsuario() {
        when(repo.findAll()).thenThrow(new CannotCreateTransactionException("Connection refused"));

        UnexoException e = falla("LISPER[\"*\"]");
        assertEquals(ErrorCode.BD_SIN_CONEXION, e.getError());
        assertEquals(Accion.RESPONDER, e.getError().accion());
        assertTrue(e.getMessage().contains("No hay conexion con la base de datos por el momento."), e.getMessage());
    }

    @Test
    void bdTimeout502() {
        when(repo.findAll()).thenThrow(new QueryTimeoutException("timeout"));

        assertEquals(ErrorCode.BD_TIMEOUT, falla("LISPER").getError());
    }

    @Test
    void restriccionDeBd503() {
        when(repo.save(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertEquals(ErrorCode.BD_RESTRICCION, falla(INSPER_VALIDO).getError());
    }

    @Test
    void errorSql504() {
        when(repo.findAll()).thenThrow(new InvalidDataAccessResourceUsageException("relation \"persona\" does not exist"));

        assertEquals(ErrorCode.BD_ERROR_SQL, falla("LISPER").getError());
    }

    @Test
    void cualquierOtraCosa599() {
        when(repo.findAll()).thenThrow(new IllegalStateException("algo que nadie previo"));

        UnexoException e = falla("LISPER");
        assertEquals(ErrorCode.ERROR_INTERNO, e.getError());
        assertEquals(Accion.RESPONDER, e.getError().accion());
    }
}
