package com.corre_email.unexo.exception;

/**
 * Catalogo de TODOS los errores del sistema. El numero indica la etapa donde fallo:
 * <ul>
 * <li>1xx - Sintaxis del asunto (parser)</li>
 * <li>2xx - Comando y permisos</li>
 * <li>3xx - Parametros</li>
 * <li>4xx - Reglas de negocio</li>
 * <li>5xx - Infraestructura (base de datos, correo) y errores internos</li>
 * </ul>
 */
public enum ErrorCode {

    // ---------- 1xx: Sintaxis ----------
    ASUNTO_VACIO(101, "ASUNTO VACIO", Accion.RESPONDER),
    FALTA_NOMBRE_COMANDO(102, "FALTA EL NOMBRE DEL COMANDO", Accion.RESPONDER),
    NOMBRE_COMANDO_INVALIDO(103, "NOMBRE DE COMANDO INVALIDO", Accion.RESPONDER),
    FALTA_CORCHETE_APERTURA(104, "FALTA EL CORCHETE DE APERTURA [", Accion.RESPONDER),
    FALTA_CORCHETE_CIERRE(105, "FALTA EL CORCHETE DE CIERRE ]", Accion.RESPONDER),
    FALTA_COMILLA_APERTURA(106, "FALTA UNA COMILLA DE APERTURA", Accion.RESPONDER),
    FALTA_COMILLA_CIERRE(107, "FALTA UNA COMILLA DE CIERRE", Accion.RESPONDER),
    FALTA_COMA(108, "FALTA UNA COMA", Accion.RESPONDER),
    COMA_SOBRANTE(109, "COMA SOBRANTE", Accion.RESPONDER),
    SEPARADOR_INCORRECTO(110, "SEPARADOR INCORRECTO", Accion.RESPONDER),
    TEXTO_DESPUES_DEL_CIERRE(111, "TEXTO DESPUES DEL CORCHETE DE CIERRE", Accion.RESPONDER),
    CARACTER_INESPERADO(112, "CARACTER INESPERADO", Accion.RESPONDER),

    // ---------- 2xx: Comando y permisos ----------
    COMANDO_NO_EXISTE(201, "EL COMANDO NO EXISTE", Accion.RESPONDER),
    REMITENTE_NO_REGISTRADO(202, "REMITENTE NO REGISTRADO", Accion.RESPONDER),
    USUARIO_INACTIVO(203, "USUARIO INACTIVO", Accion.RESPONDER),
    SIN_PERMISO(204, "SIN PERMISO PARA ESTE COMANDO", Accion.RESPONDER),

    // ---------- 3xx: Parametros ----------
    ERRORES_EN_PARAMETROS(300, "HAY PARAMETROS CON ERRORES", Accion.RESPONDER),
    FALTAN_PARAMETROS(301, "FALTAN PARAMETROS", Accion.RESPONDER),
    SOBRAN_PARAMETROS(302, "SOBRAN PARAMETROS", Accion.RESPONDER),
    PARAMETRO_VACIO(303, "PARAMETRO OBLIGATORIO VACIO", Accion.RESPONDER),
    TIPO_INCORRECTO(304, "TIPO DE DATO INCORRECTO", Accion.RESPONDER),
    FORMATO_INCORRECTO(305, "FORMATO INCORRECTO", Accion.RESPONDER),
    LARGO_FUERA_DE_RANGO(306, "LARGO FUERA DE RANGO", Accion.RESPONDER),
    VALOR_NO_PERMITIDO(307, "VALOR NO PERMITIDO", Accion.RESPONDER),
    COMODIN_NO_PERMITIDO(308, "COMODIN * NO PERMITIDO", Accion.RESPONDER),

    // ---------- 4xx: Reglas de negocio ----------
    REGISTRO_DUPLICADO(401, "REGISTRO DUPLICADO", Accion.RESPONDER),
    REGISTRO_NO_ENCONTRADO(402, "REGISTRO NO ENCONTRADO", Accion.RESPONDER),
    DATO_UNICO_REPETIDO(403, "DATO YA REGISTRADO", Accion.RESPONDER),
    REFERENCIA_INVALIDA(404, "REFERENCIA INVALIDA", Accion.RESPONDER),
    OPERACION_NO_PERMITIDA(405, "OPERACION NO PERMITIDA", Accion.RESPONDER),

    // ---------- 5xx: Infraestructura ----------
    // Con la BD caida se le avisa al usuario (y no se reintenta en silencio) para que
    // sepa que su comando NO se hizo y lo mande de nuevo mas tarde.
    BD_SIN_CONEXION(501, "SIN CONEXION CON LA BASE DE DATOS", Accion.RESPONDER),
    BD_TIMEOUT(502, "LA BASE DE DATOS NO RESPONDE", Accion.RESPONDER),
    BD_RESTRICCION(503, "RESTRICCION DE BASE DE DATOS", Accion.RESPONDER),
    BD_ERROR_SQL(504, "ERROR EN LA BASE DE DATOS", Accion.RESPONDER),
    // Con el servidor de correo caido no hay forma de avisarle a nadie por correo:
    // solo queda la consola, y el correo se atiende cuando el servidor vuelva.
    POP3_SIN_CONEXION(511, "EL SERVIDOR DE CORREO NO RESPONDE", Accion.SOLO_LOG),
    POP3_AUTENTICACION(512, "USUARIO O CLAVE DE CORREO INCORRECTOS", Accion.SOLO_LOG),
    SMTP_SIN_CONEXION(513, "EL SERVIDOR DE CORREO NO RESPONDE (ENVIO)", Accion.REINTENTAR),
    SMTP_DESTINATARIO_RECHAZADO(514, "DESTINATARIO RECHAZADO", Accion.DESCARTAR),
    CORREO_ILEGIBLE(515, "CORREO ILEGIBLE", Accion.DESCARTAR),
    RESPUESTA_AUTOMATICA(516, "RESPUESTA AUTOMATICA O REBOTE", Accion.DESCARTAR),
    ERROR_INTERNO(599, "ERROR INTERNO DEL SISTEMA", Accion.RESPONDER);

    private final int codigo;
    private final String titulo;
    private final Accion accion;

    ErrorCode(int codigo, String titulo, Accion accion) {
        this.codigo = codigo;
        this.titulo = titulo;
        this.accion = accion;
    }

    public int codigo() {
        return codigo;
    }

    public String titulo() {
        return titulo;
    }

    public Accion accion() {
        return accion;
    }
}
