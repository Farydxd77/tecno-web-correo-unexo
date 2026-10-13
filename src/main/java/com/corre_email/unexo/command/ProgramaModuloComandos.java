package com.corre_email.unexo.command;

import com.corre_email.unexo.exception.ErrorCode;
import com.corre_email.unexo.exception.UnexoException;
import com.corre_email.unexo.model.Modulo;
import com.corre_email.unexo.model.Programa;
import com.corre_email.unexo.model.ProgramaModulo;
import com.corre_email.unexo.repository.ModuloRepository;
import com.corre_email.unexo.repository.ProgramaModuloRepository;
import com.corre_email.unexo.repository.ProgramaRepository;
import com.corre_email.unexo.validation.DefinicionComando;
import com.corre_email.unexo.validation.Parametros;
import com.corre_email.unexo.validation.Reglas;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.corre_email.unexo.validation.ParamDef.obligatorio;

/** CRUD de la tabla programa_modulo (que modulos tiene cada programa): LISPMD, VERPMD, INSPMD, MODPMD, ELIPMD. */
@Configuration(proxyBeanMethods = false)
public class ProgramaModuloComandos {

    private static final String ENTIDAD = "la asignacion programa-modulo";

    private final ProgramaModuloRepository repo;
    private final ProgramaRepository programas;
    private final ModuloRepository modulos;

    public ProgramaModuloComandos(ProgramaModuloRepository repo, ProgramaRepository programas,
            ModuloRepository modulos) {
        this.repo = repo;
        this.programas = programas;
        this.modulos = modulos;
    }

    // ---------- Definiciones ----------

    private static DefinicionComando conDatos(DefinicionComando definicion) {
        return definicion
                .param(Crud.id("programa_id"))
                .param(Crud.id("modulo_id"))
                .param(obligatorio("orden", Reglas.numero(), Reglas.rango(1, 100)));
    }

    // ---------- Comandos ----------

    @Bean
    public CommandStrategy lispmd() {
        return new Comando(Crud.listar("LISPMD", "Lista los modulos asignados a cada programa."),
                p -> Crud.listar("PROGRAMA-MODULO", repo.findAll(), ProgramaModuloComandos::resumen));
    }

    @Bean
    public CommandStrategy verpmd() {
        return new Comando(Crud.porId("VERPMD", "Muestra una asignacion programa-modulo por su id."),
                p -> resumen(Crud.buscar(repo, p.id("id"), ENTIDAD)));
    }

    @Bean
    public CommandStrategy inspmd() {
        return new Comando(conDatos(DefinicionComando.de("INSPMD", "Agrega un modulo a un programa, en el orden indicado."))
                .ejemplo("INSPMD[\"1\",\"3\",\"1\"]"),
                this::insertar);
    }

    @Bean
    public CommandStrategy modpmd() {
        return new Comando(conDatos(DefinicionComando.de("MODPMD", "Modifica una asignacion (el primer dato es su id).")
                .param(Crud.id("id")))
                .ejemplo("MODPMD[\"1\",\"1\",\"3\",\"2\"]"),
                this::modificar);
    }

    @Bean
    public CommandStrategy elipmd() {
        return new Comando(Crud.porId("ELIPMD", "Quita un modulo de un programa (id de la asignacion)."),
                p -> Crud.eliminar(repo, p.id("id"), ENTIDAD));
    }

    // ---------- Logica ----------

    private String insertar(Parametros p) {
        Long programaId = p.id("programa_id");
        Long moduloId = p.id("modulo_id");
        Programa programa = Crud.referencia(programas, programaId, "programa_id");
        Modulo modulo = Crud.referencia(modulos, moduloId, "modulo_id");
        if (repo.existsByProgramaIdAndModuloId(programaId, moduloId)) {
            throw new UnexoException(ErrorCode.REGISTRO_DUPLICADO,
                    "El modulo " + moduloId + " ya esta asignado al programa " + programaId + ".");
        }

        ProgramaModulo asignacion = new ProgramaModulo(programa, modulo, p.entero("orden"));
        repo.save(asignacion);
        return "OK - Modulo asignado al programa.\n" + resumen(asignacion);
    }

    private String modificar(Parametros p) {
        Long id = p.id("id");
        ProgramaModulo asignacion = Crud.buscar(repo, id, ENTIDAD);
        Long programaId = p.id("programa_id");
        Long moduloId = p.id("modulo_id");
        Programa programa = Crud.referencia(programas, programaId, "programa_id");
        Modulo modulo = Crud.referencia(modulos, moduloId, "modulo_id");
        if (repo.existsByProgramaIdAndModuloIdAndIdNot(programaId, moduloId, id)) {
            throw new UnexoException(ErrorCode.REGISTRO_DUPLICADO,
                    "El modulo " + moduloId + " ya esta asignado al programa " + programaId + ".");
        }

        asignacion.setPrograma(programa);
        asignacion.setModulo(modulo);
        asignacion.setOrden(p.entero("orden"));
        repo.save(asignacion);
        return "OK - Asignacion modificada correctamente.\n" + resumen(asignacion);
    }

    // ---------- Formato ----------

    private static String resumen(ProgramaModulo x) {
        return "ID: " + x.getId()
                + " | Programa: " + x.getPrograma().getId() + " " + x.getPrograma().getNombre()
                + " | Modulo: " + x.getModulo().getId() + " " + x.getModulo().getNombre()
                + " | Orden: " + x.getOrden();
    }
}
