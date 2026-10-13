package com.corre_email.unexo.command;

import com.corre_email.unexo.model.Cohorte;
import com.corre_email.unexo.repository.CohorteRepository;
import com.corre_email.unexo.repository.ProgramaRepository;
import com.corre_email.unexo.validation.DefinicionComando;
import com.corre_email.unexo.validation.Parametros;
import com.corre_email.unexo.validation.Reglas;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;

import static com.corre_email.unexo.validation.ParamDef.obligatorio;

/** CRUD de la tabla cohorte: LISCOH, VERCOH, INSCOH, MODCOH, ELICOH. */
@Configuration(proxyBeanMethods = false)
public class CohorteComandos {

    private static final String ENTIDAD = "la cohorte";

    private final CohorteRepository repo;
    private final ProgramaRepository programas;

    public CohorteComandos(CohorteRepository repo, ProgramaRepository programas) {
        this.repo = repo;
        this.programas = programas;
    }

    // ---------- Definiciones ----------

    private static DefinicionComando conDatos(DefinicionComando definicion) {
        return definicion
                .param(Crud.id("programa_id"))
                .param(obligatorio("nombre", Reglas.largo(2, 100)))
                .param(obligatorio("fecha_inicio", Reglas.fecha()))
                .param(obligatorio("fecha_fin", Reglas.fecha()))
                .param(obligatorio("estado", Reglas.unoDe("PLANIFICADA", "EN_CURSO", "FINALIZADA")));
    }

    // ---------- Comandos ----------

    @Bean
    public CommandStrategy liscoh() {
        return new Comando(Crud.listar("LISCOH", "Lista todas las cohortes."),
                p -> Crud.listar("COHORTES", repo.findAll(), CohorteComandos::resumen));
    }

    @Bean
    public CommandStrategy vercoh() {
        return new Comando(Crud.porId("VERCOH", "Muestra una cohorte por su id."),
                p -> resumen(Crud.buscar(repo, p.id("id"), ENTIDAD)));
    }

    @Bean
    public CommandStrategy inscoh() {
        return new Comando(conDatos(DefinicionComando.de("INSCOH", "Registra una nueva cohorte de un programa."))
                .ejemplo("INSCOH[\"1\",\"Cohorte 2026-I\",\"2026-03-01\",\"2026-07-31\",\"PLANIFICADA\"]"),
                this::insertar);
    }

    @Bean
    public CommandStrategy modcoh() {
        return new Comando(conDatos(DefinicionComando.de("MODCOH", "Modifica una cohorte (el primer dato es su id).")
                .param(Crud.id("id")))
                .ejemplo("MODCOH[\"1\",\"1\",\"Cohorte 2026-I\",\"2026-03-01\",\"2026-07-31\",\"EN_CURSO\"]"),
                this::modificar);
    }

    @Bean
    public CommandStrategy elicoh() {
        return new Comando(Crud.porId("ELICOH", "Elimina una cohorte por su id."),
                p -> Crud.eliminar(repo, p.id("id"), ENTIDAD));
    }

    // ---------- Logica ----------

    private String insertar(Parametros p) {
        Cohorte cohorte = new Cohorte();
        copiarDatos(p, cohorte);
        repo.save(cohorte);
        return "OK - Cohorte registrada con id " + cohorte.getId() + ".\n" + resumen(cohorte);
    }

    private String modificar(Parametros p) {
        Cohorte cohorte = Crud.buscar(repo, p.id("id"), ENTIDAD);
        copiarDatos(p, cohorte);
        repo.save(cohorte);
        return "OK - Cohorte modificada correctamente.\n" + resumen(cohorte);
    }

    private void copiarDatos(Parametros p, Cohorte cohorte) {
        LocalDate inicio = p.fecha("fecha_inicio");
        LocalDate fin = p.fecha("fecha_fin");
        Crud.exigir(!fin.isBefore(inicio), "La fecha_fin (" + fin + ") no puede ser anterior a la fecha_inicio (" + inicio + ").");

        cohorte.setPrograma(Crud.referencia(programas, p.id("programa_id"), "programa_id"));
        cohorte.setNombre(p.get("nombre"));
        cohorte.setFechaInicio(inicio);
        cohorte.setFechaFin(fin);
        cohorte.setEstado(p.get("estado"));
    }

    // ---------- Formato ----------

    private static String resumen(Cohorte x) {
        return "ID: " + x.getId() + " | " + x.getNombre()
                + " | Programa: " + x.getPrograma().getId() + " " + x.getPrograma().getNombre()
                + " | Del " + x.getFechaInicio() + " al " + x.getFechaFin()
                + " | " + x.getEstado();
    }
}
