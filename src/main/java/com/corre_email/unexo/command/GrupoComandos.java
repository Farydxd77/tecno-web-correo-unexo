package com.corre_email.unexo.command;

import com.corre_email.unexo.model.Grupo;
import com.corre_email.unexo.repository.CohorteRepository;
import com.corre_email.unexo.repository.GrupoRepository;
import com.corre_email.unexo.repository.ModuloRepository;
import com.corre_email.unexo.repository.PersonaRepository;
import com.corre_email.unexo.validation.DefinicionComando;
import com.corre_email.unexo.validation.Parametros;
import com.corre_email.unexo.validation.Reglas;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;

import static com.corre_email.unexo.validation.ParamDef.obligatorio;
import static com.corre_email.unexo.validation.ParamDef.opcional;

/** CRUD de la tabla grupo: LISGRU, VERGRU, INSGRU, MODGRU, ELIGRU. */
@Configuration(proxyBeanMethods = false)
public class GrupoComandos {

    private static final String ENTIDAD = "el grupo";

    private final GrupoRepository repo;
    private final CohorteRepository cohortes;
    private final ModuloRepository modulos;
    private final PersonaRepository personas;

    public GrupoComandos(GrupoRepository repo, CohorteRepository cohortes, ModuloRepository modulos,
            PersonaRepository personas) {
        this.repo = repo;
        this.cohortes = cohortes;
        this.modulos = modulos;
        this.personas = personas;
    }

    // ---------- Definiciones ----------

    private static DefinicionComando conDatos(DefinicionComando definicion) {
        return definicion
                .param(Crud.id("cohorte_id"))
                .param(Crud.id("modulo_id"))
                .param(obligatorio("docente_ci", Reglas.numero(), Reglas.largo(5, 10)))
                .param(obligatorio("version", Reglas.largo(1, 20)))
                .param(obligatorio("fecha_inicio", Reglas.fecha()))
                .param(obligatorio("fecha_fin", Reglas.fecha()))
                .param(obligatorio("cupo_minimo", Reglas.numero(), Reglas.rango(1, 1000)))
                .param(obligatorio("cupo_maximo", Reglas.numero(), Reglas.rango(1, 1000)))
                .param(opcional("enlace_meet", Reglas.largo(5, 255)))
                .param(obligatorio("estado", Reglas.unoDe("ABIERTO", "EN_CURSO", "CERRADO")));
    }

    // ---------- Comandos ----------

    @Bean
    public CommandStrategy lisgru() {
        return new Comando(Crud.listar("LISGRU", "Lista todos los grupos."),
                p -> Crud.listar("GRUPOS", repo.findAll(), GrupoComandos::resumen));
    }

    @Bean
    public CommandStrategy vergru() {
        return new Comando(Crud.porId("VERGRU", "Muestra un grupo por su id."),
                p -> detalle(Crud.buscar(repo, p.id("id"), ENTIDAD)));
    }

    @Bean
    public CommandStrategy insgru() {
        return new Comando(conDatos(DefinicionComando.de("INSGRU", "Registra un nuevo grupo (un modulo dictado en una cohorte)."))
                .ejemplo("INSGRU[\"1\",\"2\",\"12345678\",\"V1\",\"2026-03-02\",\"2026-04-10\",\"5\",\"30\",\"https://meet.google.com/abc-defg-hij\",\"ABIERTO\"]"),
                this::insertar);
    }

    @Bean
    public CommandStrategy modgru() {
        return new Comando(conDatos(DefinicionComando.de("MODGRU", "Modifica un grupo (el primer dato es su id).")
                .param(Crud.id("id")))
                .ejemplo("MODGRU[\"1\",\"1\",\"2\",\"12345678\",\"V1\",\"2026-03-02\",\"2026-04-10\",\"5\",\"35\",\"\",\"EN_CURSO\"]"),
                this::modificar);
    }

    @Bean
    public CommandStrategy eligru() {
        return new Comando(Crud.porId("ELIGRU", "Elimina un grupo por su id."),
                p -> Crud.eliminar(repo, p.id("id"), ENTIDAD));
    }

    // ---------- Logica ----------

    private String insertar(Parametros p) {
        Grupo grupo = new Grupo();
        copiarDatos(p, grupo);
        repo.save(grupo);
        return "OK - Grupo registrado con id " + grupo.getId() + ".\n" + detalle(grupo);
    }

    private String modificar(Parametros p) {
        Grupo grupo = Crud.buscar(repo, p.id("id"), ENTIDAD);
        copiarDatos(p, grupo);
        repo.save(grupo);
        return "OK - Grupo modificado correctamente.\n" + detalle(grupo);
    }

    private void copiarDatos(Parametros p, Grupo grupo) {
        LocalDate inicio = p.fecha("fecha_inicio");
        LocalDate fin = p.fecha("fecha_fin");
        Integer cupoMinimo = p.entero("cupo_minimo");
        Integer cupoMaximo = p.entero("cupo_maximo");
        Crud.exigir(!fin.isBefore(inicio), "La fecha_fin (" + fin + ") no puede ser anterior a la fecha_inicio (" + inicio + ").");
        Crud.exigir(cupoMinimo <= cupoMaximo, "El cupo_minimo (" + cupoMinimo + ") no puede ser mayor que el cupo_maximo (" + cupoMaximo + ").");

        grupo.setCohorte(Crud.referencia(cohortes, p.id("cohorte_id"), "cohorte_id"));
        grupo.setModulo(Crud.referencia(modulos, p.id("modulo_id"), "modulo_id"));
        grupo.setDocente(Crud.referencia(personas, p.get("docente_ci"), "docente_ci"));
        grupo.setVersion(p.get("version"));
        grupo.setFechaInicio(inicio);
        grupo.setFechaFin(fin);
        grupo.setCupoMinimo(cupoMinimo);
        grupo.setCupoMaximo(cupoMaximo);
        grupo.setEnlaceMeet(p.opcional("enlace_meet"));
        grupo.setEstado(p.get("estado"));
    }

    // ---------- Formato ----------

    private static String resumen(Grupo x) {
        return "ID: " + x.getId() + " | " + x.getModulo().getNombre() + " " + x.getVersion()
                + " | Cohorte: " + x.getCohorte().getNombre()
                + " | Docente: " + x.getDocente().getNombre() + " " + x.getDocente().getApellido()
                + " | " + x.getEstado();
    }

    private static String detalle(Grupo x) {
        return "ID: " + x.getId()
                + "\nModulo: " + x.getModulo().getId() + " " + x.getModulo().getNombre()
                + "\nCohorte: " + x.getCohorte().getId() + " " + x.getCohorte().getNombre()
                + "\nDocente: " + x.getDocente().getCi() + " " + x.getDocente().getNombre() + " " + x.getDocente().getApellido()
                + "\nVersion: " + x.getVersion()
                + "\nFechas: del " + x.getFechaInicio() + " al " + x.getFechaFin()
                + "\nCupo: minimo " + x.getCupoMinimo() + ", maximo " + x.getCupoMaximo()
                + "\nEnlace Meet: " + Crud.texto(x.getEnlaceMeet())
                + "\nEstado: " + x.getEstado();
    }
}
