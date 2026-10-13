package com.corre_email.unexo.exception;

import com.corre_email.unexo.parser.AsuntoParser;
import jakarta.mail.Address;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;
import jakarta.mail.internet.InternetAddress;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErroresTest {

    @Test
    void losCodigosNoSeRepitenYRespetanLaEtapa() {
        Set<Integer> vistos = new HashSet<>();
        for (ErrorCode error : ErrorCode.values()) {
            assertTrue(vistos.add(error.codigo()), "Codigo repetido: " + error.codigo());
            assertTrue(error.codigo() >= 100 && error.codigo() < 600, error.name());
        }
    }

    @Test
    void laFlechaQuedaDebajoDelError() {
        UnexoException e = assertThrows(UnexoException.class,
                () -> AsuntoParser.parsear("INSPER[\"123\",\"Juan\" \"Perez\"]"));

        String texto = FormateadorRespuesta.formatear(e);
        List<String> lineas = Arrays.asList(texto.split("\n"));
        int iComando = lineas.indexOf("  INSPER[\"123\",\"Juan\" \"Perez\"]");

        assertTrue(texto.startsWith("ERROR 108 - FALTA UNA COMA"), texto);
        assertEquals('"', lineas.get(iComando).charAt(lineas.get(iComando + 1).indexOf('^')), texto);
        assertTrue(texto.contains("Posicion: 21 | Cerca de: "), texto);
        assertTrue(texto.endsWith("Envie un correo con asunto HELP para ver todos los comandos."), texto);
    }

    @Test
    void variosErroresSeListanConSuCodigo() {
        UnexoException e = new UnexoException(ErrorCode.ERRORES_EN_PARAMETROS, "Corrija los siguientes parametros:")
                .conErrores(List.of(
                        new UnexoException(ErrorCode.TIPO_INCORRECTO, "Parametro 1 (ci): mal"),
                        new UnexoException(ErrorCode.FORMATO_INCORRECTO, "Parametro 7 (email): mal")));

        String texto = FormateadorRespuesta.formatear(e);

        assertTrue(texto.startsWith("ERROR 300 - HAY 2 PARAMETROS CON ERRORES"), texto);
        assertTrue(texto.contains("- [304] Parametro 1 (ci): mal\n- [305] Parametro 7 (email): mal"), texto);
    }

    @Test
    void erroresDeCorreoSeTraducen() throws Exception {
        Address[] invalidas = { new InternetAddress("alguien@gmail.com") };
        SendFailedException rechazo = new SendFailedException("Relaying denied", null, null, null, invalidas);

        assertEquals(ErrorCode.SMTP_DESTINATARIO_RECHAZADO,
                TraductorExcepciones.traducir(new MailSendException(Map.of("msg", rechazo))).getError());
        assertEquals(ErrorCode.SMTP_SIN_CONEXION,
                TraductorExcepciones.traducir(new MailSendException("Connection refused")).getError());
        assertEquals(ErrorCode.POP3_AUTENTICACION,
                TraductorExcepciones.traducir(new AuthenticationFailedException("bad password")).getError());
        assertEquals(ErrorCode.POP3_SIN_CONEXION,
                TraductorExcepciones.traducir(new MessagingException("Connect failed")).getError());
    }
}
