package com.corre_email.unexo.service;

import com.corre_email.unexo.exception.ErrorCode;
import com.corre_email.unexo.exception.UnexoException;
import jakarta.mail.SendFailedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BitacoraTest {

    private final PrintStream original = System.out;
    private final ByteArrayOutputStream salida = new ByteArrayOutputStream();

    @BeforeEach
    void capturarConsola() {
        System.setOut(new PrintStream(salida, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void devolverConsola() {
        System.setOut(original);
        System.out.print(salida.toString(StandardCharsets.UTF_8));
    }

    @Test
    void bloqueDeUnCorreoConErrorYEnvioRechazado() {
        SendFailedException rechazo = new SendFailedException("Invalid Addresses",
                new SendFailedException("550 5.7.1 <x@gmail.com>... Relaying denied"));
        MailSendException envio = new MailSendException(Map.of("mensaje", rechazo));

        Bitacora.correo("x@gmail.com", "!hola señor!")
                .error("Resultado", new UnexoException(ErrorCode.NOMBRE_COMANDO_INVALIDO,
                        "El asunto debe empezar con el nombre de un comando (por ejemplo HELP) y empieza con '!'."))
                .error("Envio", new UnexoException(ErrorCode.SMTP_DESTINATARIO_RECHAZADO,
                        "El servidor rechazo la direccion de destino (solo se puede responder a @tecnoweb.org.bo).", envio))
                .fila("Buzon", "se borra: el usuario NO recibira respuesta")
                .imprimir();

        String texto = salida.toString(StandardCharsets.UTF_8);
        assertTrue(texto.startsWith("+-- CORREO "), texto);
        assertTrue(texto.contains("| De       : x@gmail.com\n"), texto);
        assertTrue(texto.contains("| Resultado: ERROR 103 - NOMBRE DE COMANDO INVALIDO\n"
                + "|            El asunto debe empezar"), texto);
        // La causa de fondo, no el texto enorme de Spring
        assertTrue(texto.contains("| Detalle  : 550 5.7.1 <x@gmail.com>... Relaying denied\n"), texto);
        assertTrue(texto.contains("| Buzon    : se borra"), texto);
    }

    @Test
    void causaRealDesenvuelveHastaElFondo() {
        Exception e = new RuntimeException("envoltorio", new IllegalStateException("capa",
                new java.net.ConnectException("Connection refused: getsockopt")));
        assertEquals("Connection refused: getsockopt", Bitacora.causaReal(e));
    }
}
