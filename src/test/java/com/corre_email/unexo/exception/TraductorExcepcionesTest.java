package com.corre_email.unexo.exception;

import jakarta.mail.MessagingException;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import org.springframework.transaction.TransactionSystemException;

import java.net.ConnectException;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Errores que llegan "envueltos" dentro de otros y los mensajes que ve el usuario. */
class TraductorExcepcionesTest {

    private static ErrorCode codigo(Throwable e) {
        return TraductorExcepciones.traducir(e).getError();
    }

    @Test
    void conexionCaidaEnvueltaEnUnaTransaccion() {
        TransactionSystemException e = new TransactionSystemException("Could not commit JPA transaction");
        e.initCause(new SQLException("Connection refused", "08001"));

        assertEquals(ErrorCode.BD_SIN_CONEXION, codigo(e));
    }

    @Test
    void errorDeHibernateSinTraducir() {
        assertEquals(ErrorCode.BD_SIN_CONEXION,
                codigo(new PersistenceException("x", new SQLException("Connection refused", "08006"))));
        assertEquals(ErrorCode.BD_ERROR_SQL,
                codigo(new PersistenceException("x", new SQLException("syntax error", "42601"))));
    }

    @Test
    void errorDeCorreoEnvueltoEnOtro() {
        RuntimeException e = new IllegalStateException("envoltorio", new MessagingException("x", new ConnectException()));
        assertEquals(ErrorCode.POP3_SIN_CONEXION, codigo(e));
    }

    @Test
    void mensajesParaElUsuario() {
        UnexoException bd = TraductorExcepciones.traducir(new SQLException("Connection refused", "08001"));
        assertTrue(bd.getMessage().startsWith("No hay conexion con la base de datos por el momento."), bd.getMessage());
        assertTrue(bd.getMessage().contains("Su comando NO se realizo"), bd.getMessage());

        UnexoException correo = TraductorExcepciones.traducir(new MailSendException("Connection refused"));
        assertEquals("El servidor de correo no responde por el momento.", correo.getMessage());
    }

    @Test
    void loDesconocidoSigueSiendo599() {
        assertEquals(ErrorCode.ERROR_INTERNO, codigo(new IllegalStateException("nadie lo previo")));
    }
}
