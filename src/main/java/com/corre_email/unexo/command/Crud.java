package com.corre_email.unexo.command;

import com.corre_email.unexo.exception.ErrorCode;
import com.corre_email.unexo.exception.UnexoException;
import com.corre_email.unexo.validation.DefinicionComando;
import com.corre_email.unexo.validation.ParamDef;
import com.corre_email.unexo.validation.Reglas;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Function;

import static com.corre_email.unexo.validation.ParamDef.obligatorio;
import static com.corre_email.unexo.validation.ParamDef.opcional;

/** Lo que se repite en el CRUD de todas las tablas: definiciones, busquedas, listado y borrado. */
public final class Crud {

    private Crud() {
    }

    // ---------- Definiciones ----------

    /** LISxxx["*"]: el "*" es opcional, LISxxx solo tambien lista todo. */
    public static DefinicionComando listar(String comando, String descripcion) {
        return DefinicionComando.de(comando, descripcion)
                .param(opcional("filtro", Reglas.unoDe("*")).conComodin())
                .ejemplo(comando + "[\"*\"]");
    }

    /** VERxxx["1"] y ELIxxx["1"]: un solo parametro, el id. */
    public static DefinicionComando porId(String comando, String descripcion) {
        return DefinicionComando.de(comando, descripcion)
                .param(id("id"))
                .ejemplo(comando + "[\"1\"]");
    }

    /** Un id numerico: el propio o el de otra tabla (ej. "programa_id"). */
    public static ParamDef id(String nombre) {
        return obligatorio(nombre, Reglas.numero(), Reglas.largo(1, 9));
    }

    // ---------- Operaciones ----------

    /** Busca por id; si no existe es un 402. */
    public static <T, ID> T buscar(JpaRepository<T, ID> repo, ID id, String entidad) {
        return repo.findById(id).orElseThrow(() -> new UnexoException(ErrorCode.REGISTRO_NO_ENCONTRADO,
                "No existe " + entidad + " " + id + "."));
    }

    /** Busca un registro al que apunta un parametro (ej. el programa_id de una cohorte); si no existe es un 404. */
    public static <T, ID> T referencia(JpaRepository<T, ID> repo, ID id, String parametro) {
        return repo.findById(id).orElseThrow(() -> new UnexoException(ErrorCode.REFERENCIA_INVALIDA,
                "Parametro " + parametro + ": no existe ningun registro con id " + id + "."));
    }

    public static <T> String listar(String titulo, List<T> registros, Function<T, String> linea) {
        if (registros.isEmpty()) {
            return "No hay registros en " + titulo + ".";
        }
        StringBuilder sb = new StringBuilder(titulo + " (" + registros.size() + "):\n");
        for (T registro : registros) {
            sb.append("- ").append(linea.apply(registro)).append('\n');
        }
        return sb.toString();
    }

    /** Si otra tabla apunta a este registro, la BD no deja borrarlo: se avisa con un 405 claro. */
    public static <T, ID> String eliminar(JpaRepository<T, ID> repo, ID id, String entidad) {
        T registro = buscar(repo, id, entidad);
        try {
            repo.delete(registro);
        } catch (RuntimeException e) {
            if (!violaLlaveForanea(e)) {
                throw e;
            }
            throw new UnexoException(ErrorCode.OPERACION_NO_PERMITIDA,
                    "No se puede eliminar " + entidad + " " + id
                            + " porque hay otros registros que dependen de este dato. Eliminelos primero.", e);
        }
        return "OK - Se elimino " + entidad + " " + id + ".";
    }

    /** El error de "otra tabla apunta aqui" puede llegar envuelto en otros; se busca en toda la cadena. */
    private static boolean violaLlaveForanea(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof DataIntegrityViolationException) {
                return true;
            }
            // 23503 = foreign_key_violation en PostgreSQL
            if (t instanceof SQLException sql && "23503".equals(sql.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    /** Reglas que comparan dos parametros entre si (ej. fecha_fin no antes de fecha_inicio). */
    public static void exigir(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new UnexoException(ErrorCode.VALOR_NO_PERMITIDO, mensaje);
        }
    }

    // ---------- Formato de las respuestas ----------

    public static String texto(Object valor) {
        if (valor == null) {
            return "-";
        }
        return valor instanceof BigDecimal d ? d.toPlainString() : valor.toString();
    }

    public static String siNo(Boolean valor) {
        return valor == null ? "-" : (valor ? "SI" : "NO");
    }

    public static String activo(Boolean valor) {
        return valor == null ? "-" : (valor ? "ACTIVO" : "INACTIVO");
    }
}
