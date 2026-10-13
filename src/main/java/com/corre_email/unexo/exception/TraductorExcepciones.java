package com.corre_email.unexo.exception;

import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.internet.AddressException;
import jakarta.persistence.PersistenceException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.transaction.CannotCreateTransactionException;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;

/**
 * Convierte cualquier excepcion (de Spring, Hibernate, JavaMail, Java...) en una
 * {@link UnexoException} con su codigo. Es la red de seguridad del sistema: lo
 * que no se reconoce termina como ERROR_INTERNO (599), nunca se escapa.
 */
public final class TraductorExcepciones {

    private static final String MSG_BD_SIN_CONEXION = "No hay conexion con la base de datos por el momento.\n"
            + "Su comando NO se realizo. Por favor envielo de nuevo en unos minutos.";
    private static final String MSG_BD_TIMEOUT = "La base de datos no responde por el momento.\n"
            + "Su comando NO se realizo. Por favor envielo de nuevo en unos minutos.";
    private static final String MSG_BD_ERROR = "Ocurrio un error en la base de datos al procesar su comando. Avise al administrador.";
    private static final String MSG_CORREO_CAIDO = "El servidor de correo no responde por el momento.";

    private TraductorExcepciones() {
    }

    public static UnexoException traducir(Throwable e) {
        if (e instanceof UnexoException u) {
            return u;
        }

        // ---------- Base de datos ----------
        if (e instanceof QueryTimeoutException) {
            return new UnexoException(ErrorCode.BD_TIMEOUT, MSG_BD_TIMEOUT, e);
        }
        if (e instanceof CannotCreateTransactionException
                || e instanceof DataAccessResourceFailureException
                || e instanceof TransientDataAccessResourceException
                || e instanceof RecoverableDataAccessException
                || (e instanceof DataAccessException && esFalloDeConexion(e))) {
            return new UnexoException(ErrorCode.BD_SIN_CONEXION, MSG_BD_SIN_CONEXION, e);
        }
        if (e instanceof DataIntegrityViolationException) {
            return new UnexoException(ErrorCode.BD_RESTRICCION,
                    "Los datos enviados chocan con un registro existente (dato duplicado o relacionado).", e);
        }
        if (e instanceof DataAccessException) {
            return new UnexoException(ErrorCode.BD_ERROR_SQL, MSG_BD_ERROR, e);
        }

        // ---------- Correo ----------
        if (e instanceof MailSendException mse && destinatarioRechazado(mse)) {
            return new UnexoException(ErrorCode.SMTP_DESTINATARIO_RECHAZADO,
                    "El servidor rechazo la direccion de destino (solo se puede responder a @tecnoweb.org.bo).", e);
        }
        if (e instanceof MailException) {
            return new UnexoException(ErrorCode.SMTP_SIN_CONEXION, MSG_CORREO_CAIDO, e);
        }
        if (e instanceof AddressException
                || (e instanceof SendFailedException sfe && tieneDireccionesInvalidas(sfe))) {
            return new UnexoException(ErrorCode.SMTP_DESTINATARIO_RECHAZADO, "La direccion de destino no es valida.", e);
        }
        if (e instanceof AuthenticationFailedException) {
            return new UnexoException(ErrorCode.POP3_AUTENTICACION,
                    "El servidor de correo rechazo el usuario o la clave.", e);
        }
        if (e instanceof MessagingException) {
            return new UnexoException(ErrorCode.POP3_SIN_CONEXION, MSG_CORREO_CAIDO, e);
        }

        // ---------- Excepciones de JDBC/Hibernate que llegan sin traducir ----------
        if (e instanceof SQLException) {
            return esFalloDeConexion(e)
                    ? new UnexoException(ErrorCode.BD_SIN_CONEXION, MSG_BD_SIN_CONEXION, e)
                    : new UnexoException(ErrorCode.BD_ERROR_SQL, MSG_BD_ERROR, e);
        }
        if (e instanceof PersistenceException) {
            return esFalloDeConexion(e)
                    ? new UnexoException(ErrorCode.BD_SIN_CONEXION, MSG_BD_SIN_CONEXION, e)
                    : new UnexoException(ErrorCode.BD_ERROR_SQL, MSG_BD_ERROR, e);
        }

        // ---------- Errores "envueltos" ----------
        // Spring suele meter el error real dentro de otro (ej. TransactionSystemException
        // con una SQLException adentro). Antes de rendirse se desenvuelve la cadena de
        // causas y se traduce el primer error conocido que aparezca.
        for (Throwable t = e.getCause(); t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof DataAccessException || t instanceof SQLException || t instanceof PersistenceException
                    || t instanceof MailException || t instanceof MessagingException) {
                return traducir(t);
            }
        }

        return new UnexoException(ErrorCode.ERROR_INTERNO,
                "Ocurrio un error inesperado al procesar su comando. Intente nuevamente o avise al administrador.", e);
    }

    /** Busca en la cadena de causas un fallo de red o de conexion JDBC (SQLState 08xxx). */
    private static boolean esFalloDeConexion(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof IOException || t instanceof SQLTransientConnectionException) {
                return true;
            }
            if (t instanceof SQLException sql && sql.getSQLState() != null && sql.getSQLState().startsWith("08")) {
                return true;
            }
        }
        return false;
    }

    /** Distingue un rechazo definitivo de la direccion (ej. "Relaying denied") de una caida de red. */
    private static boolean destinatarioRechazado(MailSendException e) {
        for (Exception fallo : e.getFailedMessages().values()) {
            if (fallo instanceof SendFailedException sfe && tieneDireccionesInvalidas(sfe)) {
                return true;
            }
        }
        return false;
    }

    private static boolean tieneDireccionesInvalidas(SendFailedException e) {
        return e.getInvalidAddresses() != null && e.getInvalidAddresses().length > 0;
    }
}
