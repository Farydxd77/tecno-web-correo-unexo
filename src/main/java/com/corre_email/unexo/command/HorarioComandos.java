package com.corre_email.unexo.command;

import com.corre_email.unexo.model.Horario;
import com.corre_email.unexo.repository.GrupoRepository;
import com.corre_email.unexo.repository.HorarioRepository;
import com.corre_email.unexo.validation.DefinicionComando;
import com.corre_email.unexo.validation.Parametros;
import com.corre_email.unexo.validation.Reglas;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalTime;

import static com.corre_email.unexo.validation.ParamDef.obligatorio;

/** CRUD de la tabla horario: LISHOR, VERHOR, INSHOR, MODHOR, ELIHOR. */
@Configuration(proxyBeanMethods = false)
public class HorarioComandos {

    private static final String ENTIDAD = "el horario";

    private final HorarioRepository repo;
    private final GrupoRepository grupos;

    public HorarioComandos(HorarioRepository repo, GrupoRepository grupos) {
        this.repo = repo;
        this.grupos = grupos;
    }

    // ---------- Definiciones ----------

    private static DefinicionComando conDatos(DefinicionComando definicion) {
        return definicion
                .param(Crud.id("grupo_id"))
                .param(obligatorio("dia_semana",
                        Reglas.unoDe("LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO")))
                .param(obligatorio("hora_inicio", Reglas.hora()))
                .param(obligatorio("hora_fin", Reglas.hora()));
    }

    // ---------- Comandos ----------

    @Bean
    public CommandStrategy lishor() {
        return new Comando(Crud.listar("LISHOR", "Lista todos los horarios."),
                p -> Crud.listar("HORARIOS", repo.findAll(), HorarioComandos::resumen));
    }

    @Bean
    public CommandStrategy verhor() {
        return new Comando(Crud.porId("VERHOR", "Muestra un horario por su id."),
                p -> resumen(Crud.buscar(repo, p.id("id"), ENTIDAD)));
    }

    @Bean
    public CommandStrategy inshor() {
        return new Comando(conDatos(DefinicionComando.de("INSHOR", "Agrega un horario a un grupo."))
                .ejemplo("INSHOR[\"1\",\"LUNES\",\"19:00\",\"21:00\"]"),
                this::insertar);
    }

    @Bean
    public CommandStrategy modhor() {
        return new Comando(conDatos(DefinicionComando.de("MODHOR", "Modifica un horario (el primer dato es su id).")
                .param(Crud.id("id")))
                .ejemplo("MODHOR[\"1\",\"1\",\"MIERCOLES\",\"19:00\",\"21:30\"]"),
                this::modificar);
    }

    @Bean
    public CommandStrategy elihor() {
        return new Comando(Crud.porId("ELIHOR", "Elimina un horario por su id."),
                p -> Crud.eliminar(repo, p.id("id"), ENTIDAD));
    }

    // ---------- Logica ----------

    private String insertar(Parametros p) {
        Horario horario = new Horario();
        copiarDatos(p, horario);
        repo.save(horario);
        return "OK - Horario registrado con id " + horario.getId() + ".\n" + resumen(horario);
    }

    private String modificar(Parametros p) {
        Horario horario = Crud.buscar(repo, p.id("id"), ENTIDAD);
        copiarDatos(p, horario);
        repo.save(horario);
        return "OK - Horario modificado correctamente.\n" + resumen(horario);
    }

    private void copiarDatos(Parametros p, Horario horario) {
        LocalTime inicio = p.hora("hora_inicio");
        LocalTime fin = p.hora("hora_fin");
        Crud.exigir(fin.isAfter(inicio), "La hora_fin (" + fin + ") debe ser despues de la hora_inicio (" + inicio + ").");

        horario.setGrupo(Crud.referencia(grupos, p.id("grupo_id"), "grupo_id"));
        horario.setDiaSemana(p.get("dia_semana"));
        horario.setHoraInicio(inicio);
        horario.setHoraFin(fin);
    }

    // ---------- Formato ----------

    private static String resumen(Horario x) {
        return "ID: " + x.getId() + " | Grupo: " + x.getGrupo().getId()
                + " " + x.getGrupo().getModulo().getNombre() + " " + x.getGrupo().getVersion()
                + " | " + x.getDiaSemana() + " de " + x.getHoraInicio() + " a " + x.getHoraFin();
    }
}
