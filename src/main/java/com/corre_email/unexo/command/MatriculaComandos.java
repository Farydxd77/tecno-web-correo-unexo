package com.corre_email.unexo.command;

import com.corre_email.unexo.exception.ErrorCode;
import com.corre_email.unexo.exception.UnexoException;
import com.corre_email.unexo.model.Grupo;
import com.corre_email.unexo.model.Matricula;
import com.corre_email.unexo.repository.GrupoRepository;
import com.corre_email.unexo.repository.MatriculaRepository;
import com.corre_email.unexo.repository.PersonaRepository;
import com.corre_email.unexo.validation.DefinicionComando;
import com.corre_email.unexo.validation.Parametros;
import com.corre_email.unexo.validation.Reglas;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.corre_email.unexo.validation.ParamDef.obligatorio;
import static com.corre_email.unexo.validation.ParamDef.opcional;

/** CRUD de la tabla matricula (inscripcion de una persona en un grupo): LISMAT, VERMAT, INSMAT, MODMAT, ELIMAT. */
@Configuration(proxyBeanMethods = false)
public class MatriculaComandos {

    private static final String ENTIDAD = "la matricula";
    private static final String ANULADA = "ANULADA";

    private final MatriculaRepository repo;
    private final PersonaRepository personas;
    private final GrupoRepository grupos;

    public MatriculaComandos(MatriculaRepository repo, PersonaRepository personas, GrupoRepository grupos) {
        this.repo = repo;
        this.personas = personas;
        this.grupos = grupos;
    }

    // ---------- Definiciones ----------

    private static DefinicionComando conDatos(DefinicionComando definicion) {
        return definicion
                .param(obligatorio("usuario_ci", Reglas.numero(), Reglas.largo(5, 10)))
                .param(Crud.id("grupo_id"))
                .param(obligatorio("estado", Reglas.unoDe("PENDIENTE", "ACTIVA", ANULADA)))
                .param(obligatorio("origen", Reglas.largo(2, 50)))
                .param(obligatorio("acepto_requisitos", Reglas.unoDe("SI", "NO")))
                .param(opcional("nota_final", Reglas.decimal(), Reglas.rango(0, 100)))
                .param(opcional("resultado", Reglas.unoDe("APROBADO", "REPROBADO")));
    }

    // ---------- Comandos ----------

    @Bean
    public CommandStrategy lismat() {
        return new Comando(Crud.listar("LISMAT", "Lista todas las matriculas."),
                p -> Crud.listar("MATRICULAS", repo.findAll(), MatriculaComandos::resumen));
    }

    @Bean
    public CommandStrategy vermat() {
        return new Comando(Crud.porId("VERMAT", "Muestra una matricula por su id."),
                p -> detalle(Crud.buscar(repo, p.id("id"), ENTIDAD)));
    }

    @Bean
    public CommandStrategy insmat() {
        return new Comando(conDatos(DefinicionComando.de("INSMAT", "Inscribe a una persona en un grupo."))
                .ejemplo("INSMAT[\"87654321\",\"1\",\"PENDIENTE\",\"CORREO\",\"SI\"]"),
                this::insertar);
    }

    @Bean
    public CommandStrategy modmat() {
        return new Comando(conDatos(DefinicionComando.de("MODMAT", "Modifica una matricula (el primer dato es su id).")
                .param(Crud.id("id")))
                .ejemplo("MODMAT[\"1\",\"87654321\",\"1\",\"ACTIVA\",\"CORREO\",\"SI\",\"85.50\",\"APROBADO\"]"),
                this::modificar);
    }

    @Bean
    public CommandStrategy elimat() {
        return new Comando(Crud.porId("ELIMAT", "Elimina una matricula por su id."),
                p -> Crud.eliminar(repo, p.id("id"), ENTIDAD));
    }

    // ---------- Logica ----------

    private String insertar(Parametros p) {
        String ci = p.get("usuario_ci");
        Long grupoId = p.id("grupo_id");
        if (repo.existsByUsuarioCiAndGrupoId(ci, grupoId)) {
            throw new UnexoException(ErrorCode.REGISTRO_DUPLICADO,
                    "La persona " + ci + " ya esta matriculada en el grupo " + grupoId + ".");
        }
        Matricula matricula = new Matricula();
        copiarDatos(p, matricula);
        if (!ANULADA.equals(matricula.getEstado())) {
            revisarCupo(matricula.getGrupo(), repo.countByGrupoIdAndEstadoNot(grupoId, ANULADA));
        }

        repo.save(matricula);
        return "OK - Matricula registrada con id " + matricula.getId() + ".\n" + detalle(matricula);
    }

    private String modificar(Parametros p) {
        Long id = p.id("id");
        Matricula matricula = Crud.buscar(repo, id, ENTIDAD);
        String ci = p.get("usuario_ci");
        Long grupoId = p.id("grupo_id");
        if (repo.existsByUsuarioCiAndGrupoIdAndIdNot(ci, grupoId, id)) {
            throw new UnexoException(ErrorCode.REGISTRO_DUPLICADO,
                    "La persona " + ci + " ya esta matriculada en el grupo " + grupoId + ".");
        }
        copiarDatos(p, matricula);
        if (!ANULADA.equals(matricula.getEstado())) {
            revisarCupo(matricula.getGrupo(), repo.countByGrupoIdAndEstadoNotAndIdNot(grupoId, ANULADA, id));
        }

        repo.save(matricula);
        return "OK - Matricula modificada correctamente.\n" + detalle(matricula);
    }

    /** Las matriculas anuladas no ocupan cupo. */
    private static void revisarCupo(Grupo grupo, long ocupados) {
        if (grupo.getCupoMaximo() != null && ocupados >= grupo.getCupoMaximo()) {
            throw new UnexoException(ErrorCode.OPERACION_NO_PERMITIDA,
                    "El grupo " + grupo.getId() + " ya esta lleno (cupo maximo " + grupo.getCupoMaximo() + ").");
        }
    }

    private void copiarDatos(Parametros p, Matricula matricula) {
        matricula.setUsuario(Crud.referencia(personas, p.get("usuario_ci"), "usuario_ci"));
        matricula.setGrupo(Crud.referencia(grupos, p.id("grupo_id"), "grupo_id"));
        matricula.setEstado(p.get("estado"));
        matricula.setOrigen(p.get("origen"));
        matricula.setAceptoRequisitos(p.siNo("acepto_requisitos"));
        matricula.setNotaFinal(p.decimal("nota_final"));
        matricula.setResultado(p.opcional("resultado"));
    }

    // ---------- Formato ----------

    private static String resumen(Matricula x) {
        return "ID: " + x.getId()
                + " | " + x.getUsuario().getCi() + " " + x.getUsuario().getNombre() + " " + x.getUsuario().getApellido()
                + " | Grupo: " + x.getGrupo().getId() + " " + x.getGrupo().getModulo().getNombre()
                + " | " + x.getEstado() + " | Nota: " + Crud.texto(x.getNotaFinal());
    }

    private static String detalle(Matricula x) {
        return "ID: " + x.getId()
                + "\nPersona: " + x.getUsuario().getCi() + " " + x.getUsuario().getNombre() + " " + x.getUsuario().getApellido()
                + "\nGrupo: " + x.getGrupo().getId() + " " + x.getGrupo().getModulo().getNombre() + " " + x.getGrupo().getVersion()
                + "\nEstado: " + x.getEstado()
                + "\nOrigen: " + x.getOrigen()
                + "\nAcepto requisitos: " + Crud.siNo(x.getAceptoRequisitos())
                + "\nNota final: " + Crud.texto(x.getNotaFinal())
                + "\nResultado: " + Crud.texto(x.getResultado());
    }
}
