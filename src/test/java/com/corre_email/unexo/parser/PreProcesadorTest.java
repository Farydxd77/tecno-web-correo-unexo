package com.corre_email.unexo.parser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PreProcesadorTest {

    @Test
    void asuntoComprimidoPegadoAOtroTextoSeDecodifica() {
        // JavaMail deja este caso sin decodificar; el usuario veria "=?UTF-8?Q?..."
        assertEquals("¡hola mundo", PreProcesador.normalizar("RE:=?UTF-8?Q?=C2=A1hola?= mundo"));
    }

    @Test
    void asuntoComprimidoEnBase64() {
        assertEquals("¡hola mundo!", PreProcesador.normalizar("=?UTF-8?B?wqFob2xhIG11bmRvIQ==?="));
    }

    @Test
    void variasPartesComprimidasSeguidasSeUnenSinEspacio() {
        assertEquals("INSPER[\"1\"]",
                PreProcesador.normalizar("=?UTF-8?Q?INSPER[=E2=80=9C?= =?UTF-8?Q?1=E2=80=9D]?="));
    }

    @Test
    void comprimidoMalFormadoSeDejaComoEsta() {
        assertEquals("=?charset-inventado?Q?hola?=", PreProcesador.normalizar("=?charset-inventado?Q?hola?="));
    }

    @Test
    void caracteresInvisiblesSeQuitan() {
        assertEquals("LISPER[\"*\"]", PreProcesador.normalizar("​LISPER[\"*\"]﻿"));
    }

    @Test
    void comillasCurvasYPrefijoRe() {
        assertEquals("LISPER[\"*\"]", PreProcesador.normalizar("RE: RE: LISPER[“*”]"));
    }
}
