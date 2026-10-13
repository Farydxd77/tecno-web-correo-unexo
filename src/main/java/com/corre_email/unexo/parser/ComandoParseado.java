package com.corre_email.unexo.parser;

import java.util.List;

/** Resultado del parser: el nombre del comando en mayusculas y los parametros tal cual llegaron. */
public record ComandoParseado(String nombre, List<String> parametros) {
}
