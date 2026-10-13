package com.corre_email.unexo.service;

import com.corre_email.unexo.exception.ErrorCode;
import com.corre_email.unexo.exception.FormateadorRespuesta;
import com.corre_email.unexo.exception.TraductorExcepciones;
import com.corre_email.unexo.exception.UnexoException;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
public class EmailService {

    private static final String ASUNTO_RESPUESTA = "Respuesta del Sistema Tecnoweb";

    /** Casillas que nunca son personas: contestarles puede armar un bucle de rebotes. */
    private static final Pattern REMITENTE_AUTOMATICO = Pattern.compile(
            "^(mailer-daemon|postmaster|no-?reply|do-?not-?reply)@", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRECEDENCIA_AUTOMATICA = Pattern.compile(
            "bulk|junk|list|auto_reply", Pattern.CASE_INSENSITIVE);

    private final CommandDispatcher commandDispatcher;
    private final JavaMailSender mailSender;

    @Value("${app.mail.host}")
    private String host;

    @Value("${app.mail.username}")
    private String username;

    @Value("${app.mail.password}")
    private String password;

    @Value("${app.mail.from}")
    private String remitente;

    // El buzon funciona como cola: cada correo atendido se borra del servidor.
    // Este set es solo una red de seguridad por si el borrado no llega a
    // aplicarse (por ejemplo si se corta la conexion antes del expunge).
    private final Set<String> correosProcesados = Collections.synchronizedSet(new HashSet<>());

    // Respuestas de comandos que YA se ejecutaron pero cuyo envio fallo. En el
    // reintento se reenvia esta respuesta en vez de ejecutar el comando otra vez
    // (si no, un INSPER reintentado contestaria "ya existe" en vez de "OK").
    private final Map<String, String> respuestasPendientes = new ConcurrentHashMap<>();

    private static final String LECTURA = "el servidor de correo (lectura POP3)";
    private static final String ENVIO = "el servidor de correo (envio SMTP)";
    private final Set<String> serviciosCaidos = ConcurrentHashMap.newKeySet();

    /**
     * Los datos que necesitamos de un correo, ya leidos y con el POP3 cerrado.
     * Si {@code descarte} no es null, el correo se borra sin responder.
     */
    private record CorreoEntrante(String id, String asunto, String remitente, UnexoException descarte) {
    }

    public EmailService(CommandDispatcher commandDispatcher, JavaMailSender mailSender) {
        this.commandDispatcher = commandDispatcher;
        this.mailSender = mailSender;
    }

    /**
     * Se ejecuta automáticamente cada 10 segundos. Va en tres fases a proposito:
     * leer, responder y recien despues borrar. Si se deja el POP3 abierto
     * mientras se envia por SMTP, un envio lento agota el timeout del POP3 y
     * los correos siguientes de la tanda fallan con "can't retrieve message #N".
     */
    @Scheduled(fixedDelay = 10000)
    public void revisarCorreos() {
        List<CorreoEntrante> bandeja = leerBandeja();
        if (bandeja.isEmpty()) {
            return;
        }

        Set<String> atendidos = new HashSet<>();
        for (CorreoEntrante correo : bandeja) {
            try {
                if (atender(correo)) {
                    atendidos.add(correo.id());
                }
            } catch (RuntimeException e) {
                // Red de seguridad final: atender() ya atrapa todo lo previsto, esto solo
                // salta ante un error de programacion. Se registra y se borra el correo,
                // porque si se deja en el buzon fallaria igual cada 10 s para siempre.
                Bitacora.correo(correo.remitente(), correo.asunto())
                        .error("Resultado", TraductorExcepciones.traducir(e))
                        .fila("Buzon", "se borra sin responder (error del programa: si se deja, fallaria cada 10 s)")
                        .imprimir();
                atendidos.add(correo.id());
            }
        }

        if (!atendidos.isEmpty()) {
            borrarDelServidor(atendidos);
        }
    }

    // ---------- Fase 1: leer ----------

    private List<CorreoEntrante> leerBandeja() {
        List<CorreoEntrante> bandeja = new ArrayList<>();
        Store store = null;
        Folder inbox = null;
        try {
            Session session = Session.getInstance(propiedadesPop3(), null);
            store = session.getStore("pop3");
            store.connect(host, username, password);

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);

            for (Message message : inbox.getMessages()) {
                CorreoEntrante correo = leer(message);
                if (correo != null) {
                    bandeja.add(correo);
                }
            }
            marcarActivo(LECTURA);

        } catch (Exception e) {
            // Servidor caido o clave mala (511/512): no hay a quien avisar, solo se registra
            marcarCaido(LECTURA, TraductorExcepciones.traducir(e));
        } finally {
            cerrar(inbox, store, false);
        }
        return bandeja;
    }

    /** Un correo con formato raro no debe tumbar el resto de la tanda. */
    private CorreoEntrante leer(Message message) {
        String id;
        try {
            id = idDe(message);
        } catch (Exception e) {
            Bitacora.sistema()
                    .error("Problema", new UnexoException(ErrorCode.CORREO_ILEGIBLE,
                            "No se pudo identificar un correo del buzon.", e))
                    .fila("Buzon", "queda en el buzon (sin identificador no se puede borrar)")
                    .imprimir();
            return null;
        }

        try {
            Address[] senders = message.getFrom();
            if (senders == null || senders.length == 0 || !(senders[0] instanceof InternetAddress direccion)) {
                return new CorreoEntrante(id, null, null,
                        new UnexoException(ErrorCode.CORREO_ILEGIBLE, "El correo no tiene remitente."));
            }
            String remitenteCorreo = direccion.getAddress();
            String asunto = message.getSubject();
            return new CorreoEntrante(id, asunto, remitenteCorreo, detectarAutomatico(message, remitenteCorreo, asunto));

        } catch (Exception e) {
            return new CorreoEntrante(id, null, null,
                    new UnexoException(ErrorCode.CORREO_ILEGIBLE, "No se pudo leer el correo: " + e.getMessage(), e));
        }
    }

    /**
     * Respuestas propias, rebotes y "fuera de oficina" no se contestan: seria
     * un bucle infinito de correos. Nuestras propias respuestas se filtran por
     * asunto y no solo por remitente, asi el sistema todavia se puede probar
     * mandandose un comando a uno mismo.
     */
    private UnexoException detectarAutomatico(Message message, String remitenteCorreo, String asunto)
            throws MessagingException {
        String motivo = null;
        if (remitente.equalsIgnoreCase(remitenteCorreo) && asunto != null && asunto.startsWith(ASUNTO_RESPUESTA)) {
            motivo = "Es una respuesta del propio sistema.";
        } else if (REMITENTE_AUTOMATICO.matcher(remitenteCorreo).find()) {
            motivo = "El remitente " + remitenteCorreo + " es una casilla automatica.";
        } else if (cabecera(message, "Auto-Submitted") != null
                && !"no".equalsIgnoreCase(cabecera(message, "Auto-Submitted"))) {
            motivo = "Correo automatico (Auto-Submitted: " + cabecera(message, "Auto-Submitted") + ").";
        } else if (cabecera(message, "X-Autoreply") != null || cabecera(message, "X-Autorespond") != null) {
            motivo = "Respuesta automatica (fuera de oficina).";
        } else if (cabecera(message, "Precedence") != null
                && PRECEDENCIA_AUTOMATICA.matcher(cabecera(message, "Precedence")).find()) {
            motivo = "Correo masivo o automatico (Precedence: " + cabecera(message, "Precedence") + ").";
        }
        return motivo == null ? null : new UnexoException(ErrorCode.RESPUESTA_AUTOMATICA, motivo);
    }

    // ---------- Fase 2: responder ----------

    /**
     * Cada correo deja un bloque en consola con lo que paso (ver Bitacora).
     *
     * @return true si el correo ya se puede borrar del buzon.
     */
    private boolean atender(CorreoEntrante correo) {
        Bitacora.Bloque bloque = Bitacora.correo(correo.remitente(), correo.asunto());
        if (correo.descarte() != null) {
            bloque.error("Resultado", correo.descarte()).fila("Buzon", "se borra sin responder").imprimir();
            return true;
        }
        if (correosProcesados.contains(correo.id())) {
            return true;
        }

        String respuesta = respuestasPendientes.get(correo.id());
        boolean esReintento = respuesta != null;
        if (!esReintento) {
            try {
                respuesta = commandDispatcher.ejecutar(correo.asunto());
                bloque.fila("Resultado", respuesta.lines().findFirst().orElse("OK"));
            } catch (RuntimeException e) {
                UnexoException error = TraductorExcepciones.traducir(e);
                bloque.error("Resultado", error);
                switch (error.getError().accion()) {
                    case REINTENTAR -> {
                        bloque.fila("Buzon", "queda en el buzon, se reintenta en 10 segundos").imprimir();
                        return false;
                    }
                    case DESCARTAR, SOLO_LOG -> {
                        bloque.fila("Buzon", "se borra sin responder").imprimir();
                        return true;
                    }
                    case RESPONDER -> respuesta = FormateadorRespuesta.formatear(error);
                }
            }
        }

        // La respuesta completa queda en consola aunque el envio falle (ej. el
        // servidor rechaza destinatarios fuera de @tecnoweb.org.bo)
        bloque.seccion("RESPUESTA AL USUARIO")
                .fila("Para", correo.remitente())
                .fila("Asunto", ASUNTO_RESPUESTA)
                .fila("Texto", respuesta)
                .seccion("ENVIO");

        UnexoException fallo = enviarRespuesta(correo.remitente(), respuesta);
        if (fallo == null) {
            respuestasPendientes.remove(correo.id());
            correosProcesados.add(correo.id());
            if (esReintento) {
                bloque.fila("Resultado", "ya se habia ejecutado antes; faltaba enviar la respuesta");
            }
            bloque.fila("Envio", "OK, respuesta enviada").fila("Buzon", "se borra").imprimir();
            return true;
        }
        if (fallo.getError() == ErrorCode.SMTP_DESTINATARIO_RECHAZADO) {
            bloque.error("Envio", fallo).fila("Buzon", "se borra: el usuario NO recibira respuesta").imprimir();
            return true;
        }
        // Servidor de correo caido: se guarda la respuesta para enviarla cuando vuelva.
        // El bloque se imprime solo la primera vez, no en cada reintento de 10 s.
        if (!esReintento) {
            respuestasPendientes.put(correo.id(), respuesta);
            bloque.error("Envio", fallo)
                    .fila("Buzon", "queda en el buzon; la respuesta se enviara sola cuando el servidor vuelva")
                    .imprimir();
        }
        return false;
    }

    // ---------- Fase 3: borrar ----------

    private void borrarDelServidor(Set<String> atendidos) {
        Store store = null;
        Folder inbox = null;
        try {
            Session session = Session.getInstance(propiedadesPop3(), null);
            store = session.getStore("pop3");
            store.connect(host, username, password);

            inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            for (Message message : inbox.getMessages()) {
                try {
                    if (atendidos.contains(idDe(message))) {
                        message.setFlag(Flags.Flag.DELETED, true);
                    }
                } catch (Exception e) {
                    Bitacora.linea("Aviso: no se pudo marcar un correo para borrar (" + Bitacora.causaReal(e)
                            + "). Se intentara en la proxima revision.");
                }
            }

        } catch (Exception e) {
            Bitacora.sistema()
                    .error("Problema", TraductorExcepciones.traducir(e))
                    .fila("Que pasa", "No se pudieron borrar los correos ya atendidos; se borraran en la proxima revision.")
                    .imprimir();
        } finally {
            // true = aplicar los borrados marcados (POP3 manda el DELE aqui)
            cerrar(inbox, store, true);
        }
    }

    // ---------- Apoyo ----------

    private Properties propiedadesPop3() {
        Properties props = new Properties();
        props.put("mail.store.protocol", "pop3");
        props.put("mail.pop3.host", host);
        props.put("mail.pop3.port", "110");
        props.put("mail.pop3.socketFactory.fallback", "true");
        props.put("mail.pop3.connectiontimeout", "20000");
        props.put("mail.pop3.timeout", "20000");
        // getInstance y no getDefaultInstance: getDefaultInstance devuelve una
        // sesion cacheada y las propiedades de esta llamada se ignorarian
        return props;
    }

    private String idDe(Message message) throws MessagingException {
        String messageId = cabecera(message, "Message-ID");
        if (messageId != null) {
            return messageId;
        }
        // Sin Message-ID caemos a fecha + asunto
        return message.getSentDate() + "|" + message.getSubject();
    }

    private static String cabecera(Message message, String nombre) throws MessagingException {
        String[] valores = message.getHeader(nombre);
        return valores != null && valores.length > 0 ? valores[0].trim() : null;
    }

    private void cerrar(Folder inbox, Store store, boolean aplicarBorrados) {
        try {
            if (inbox != null && inbox.isOpen()) {
                inbox.close(aplicarBorrados);
            }
        } catch (Exception e) {
            Bitacora.linea("Aviso: no se pudo cerrar la bandeja de correo (" + Bitacora.causaReal(e) + ").");
        }
        try {
            if (store != null && store.isConnected()) {
                store.close();
            }
        } catch (Exception e) {
            Bitacora.linea("Aviso: no se pudo cerrar la conexion de correo (" + Bitacora.causaReal(e) + ").");
        }
    }

    /**
     * @return null si se envio; si no, el error: 514 si el servidor rechazo la
     *         direccion (definitivo, reintentar no sirve) o 513 si el servidor
     *         no responde (pasajero, se reintenta).
     */
    private UnexoException enviarRespuesta(String destinatario, String contenido) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            message.setFrom(new InternetAddress(remitente));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
            message.setSubject(ASUNTO_RESPUESTA);
            message.setText(contenido, "UTF-8");
            // Estandar (RFC 3834): avisa a otros sistemas que no respondan
            // automaticamente a este correo, y nos permite reconocer los nuestros
            message.setHeader("Auto-Submitted", "auto-replied");

            mailSender.send(message);
            marcarActivo(ENVIO);
            return null;

        } catch (Exception e) {
            UnexoException error = TraductorExcepciones.traducir(e);
            if (error.getError() == ErrorCode.SMTP_DESTINATARIO_RECHAZADO) {
                return error;
            }
            // El aviso va en el bloque del correo; aqui solo se anota que el envio esta caido
            // para avisar en una linea cuando vuelva
            serviciosCaidos.add(ENVIO);
            return new UnexoException(ErrorCode.SMTP_SIN_CONEXION,
                    "El servidor de correo no responde por el momento: no se pudo enviar la respuesta.", e);
        }
    }

    /**
     * Mientras un servidor esta caido la revision falla cada 10 s. Se avisa una
     * sola vez cuando cae y otra cuando vuelve, para no llenar la consola.
     */
    private void marcarCaido(String servicio, UnexoException error) {
        if (serviciosCaidos.add(servicio)) {
            Bitacora.sistema()
                    .error("Problema", error)
                    .fila("Que pasa", "Se seguira intentando cada 10 segundos (sin repetir este aviso) hasta que "
                            + servicio + " vuelva a responder. Los correos no se pierden.")
                    .imprimir();
        }
    }

    private void marcarActivo(String servicio) {
        if (serviciosCaidos.remove(servicio)) {
            Bitacora.linea("OK - " + servicio + " volvio a responder.");
        }
    }
}
