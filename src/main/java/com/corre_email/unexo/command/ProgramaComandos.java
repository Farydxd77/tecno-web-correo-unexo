package com.corre_email.unexo.command;

import com.corre_email.unexo.exception.ErrorCode;
import com.corre_email.unexo.exception.UnexoException;
import com.corre_email.unexo.model.Programa;
import com.corre_email.unexo.repository.ProgramaRepository;
import com.corre_email.unexo.validation.DefinicionComando;
import com.corre_email.unexo.validation.Parametros;
import com.corre_email.unexo.validation.Reglas;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.corre_email.unexo.validation.ParamDef.obligatorio;
import static com.corre_email.unexo.validation.ParamDef.opcional;

/** CRUD de la tabla programa: LISPRG, VERPRG, INSPRG, MODPRG, ELIPRG. */
@Configuration(proxyBeanMethods = false)
public class ProgramaComandos {

    private static final String ENTIDAD = "el programa";

    private final ProgramaRepository repo;

    public ProgramaComandos(ProgramaRepository repo) {
        this.repo = repo;
    }

    // ---------- Definiciones ----------

    /** Los datos del programa, en el mismo orden para INSPRG y MODPRG. */
    private static DefinicionComando conDatos(DefinicionComando definicion) {
        return definicion
                .param(obligatorio("codigo", Reglas.largo(1, 20)))
                .param(obligatorio("nombre", Reglas.largo(2, 100)))
                .param(opcional("descripcion", Reglas.largo(1, 255)))
                .param(obligatorio("cantidad_modulos", Reglas.numero(), Reglas.rango(1, 50)))
                .param(obligatorio("precio_contado", Reglas.decimal(), Reglas.rango(0, 1000000)))
                .param(obligatorio("estado_comercial", Reglas.unoDe("ACTIVO", "INACTIVO")));
    }

    // ---------- Comandos ----------

    @Bean
    public CommandStrategy lisprg() {
        return new Comando(Crud.listar("LISPRG", "Lista todos los programas."),
                p -> Crud.listar("PROGRAMAS", repo.findAll(), ProgramaComandos::resumen));
    }

    @Bean
    public CommandStrategy verprg() {
        return new Comando(Crud.porId("VERPRG", "Muestra un programa por su id."),
                p -> detalle(Crud.buscar(repo, p.id("id"), ENTIDAD)));
    }

    @Bean
    public CommandStrategy insprg() {
        return new Comando(conDatos(DefinicionComando.de("INSPRG", "Registra un nuevo programa."))
                .ejemplo("INSPRG[\"DIP-JAVA\",\"Diplomado en Java\",\"Desde cero a Spring\",\"4\",\"1500.00\",\"ACTIVO\"]"),
                this::insertar);
    }

    @Bean
    public CommandStrategy modprg() {
        return new Comando(conDatos(DefinicionComando.de("MODPRG", "Modifica un programa (el primer dato es su id).")
                .param(Crud.id("id")))
                .ejemplo("MODPRG[\"1\",\"DIP-JAVA\",\"Diplomado en Java\",\"\",\"5\",\"1800\",\"ACTIVO\"]"),
                this::modificar);
    }

    @Bean
    public CommandStrategy eliprg() {
        return new Comando(Crud.porId("ELIPRG", "Elimina un programa por su id."),
                p -> Crud.eliminar(repo, p.id("id"), ENTIDAD));
    }

    // ---------- Logica ----------

    private String insertar(Parametros p) {
        String codigo = p.get("codigo");
        if (repo.existsByCodigoIgnoreCase(codigo)) {
            throw new UnexoException(ErrorCode.DATO_UNICO_REPETIDO, "Ya existe un programa con codigo " + codigo + ".");
        }

        Programa programa = new Programa();
        copiarDatos(p, programa);
        repo.save(programa);
        return "OK - Programa registrado con id " + programa.getId() + ".\n" + detalle(programa);
    }

    private String modificar(Parametros p) {
        Long id = p.id("id");
        Programa programa = Crud.buscar(repo, id, ENTIDAD);
        String codigo = p.get("codigo");
        if (repo.existsByCodigoIgnoreCaseAndIdNot(codigo, id)) {
            throw new UnexoException(ErrorCode.DATO_UNICO_REPETIDO, "Ya existe otro programa con codigo " + codigo + ".");
        }

        copiarDatos(p, programa);
        repo.save(programa);
        return "OK - Programa modificado correctamente.\n" + detalle(programa);
    }

    /** INSPRG y MODPRG llenan los mismos campos. */
    private static void copiarDatos(Parametros p, Programa programa) {
        programa.setCodigo(p.get("codigo"));
        programa.setNombre(p.get("nombre"));
        programa.setDescripcion(p.opcional("descripcion"));
        programa.setCantidadModulos(p.entero("cantidad_modulos"));
        programa.setPrecioContado(p.decimal("precio_contado"));
        programa.setEstadoComercial(p.get("estado_comercial").equals("ACTIVO"));
    }

    // ---------- Formato ----------

    private static String resumen(Programa x) {
        return "ID: " + x.getId() + " | " + x.getCodigo() + " | " + x.getNombre()
                + " | Modulos: " + x.getCantidadModulos() + " | Precio: " + Crud.texto(x.getPrecioContado())
                + " | " + Crud.activo(x.getEstadoComercial());
    }

    private static String detalle(Programa x) {
        return "ID: " + x.getId()
                + "\nCodigo: " + x.getCodigo()
                + "\nNombre: " + x.getNombre()
                + "\nDescripcion: " + Crud.texto(x.getDescripcion())
                + "\nCantidad de modulos: " + x.getCantidadModulos()
                + "\nPrecio al contado: " + Crud.texto(x.getPrecioContado())
                + "\nEstado comercial: " + Crud.activo(x.getEstadoComercial());
    }
}
