package com.corre_email.unexo.exception;

/** Que hace EmailService con el correo cuando ocurre un error. */
public enum Accion {
    /** Se le responde al usuario explicando el error y se borra el correo. */
    RESPONDER,
    /** No se responde: el correo queda en el buzon y se reintenta en la proxima revision. */
    REINTENTAR,
    /** No se responde y se borra el correo (no tiene arreglo y responder no sirve). */
    DESCARTAR,
    /** No hay un correo al que atribuir el error (ej. servidor caido): solo se registra. */
    SOLO_LOG
}
