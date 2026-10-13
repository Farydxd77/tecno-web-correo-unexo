package com.corre_email.unexo.command;

import com.corre_email.unexo.exception.ErrorCode;
import com.corre_email.unexo.exception.UnexoException;
import com.corre_email.unexo.model.Modulo;
import com.corre_email.unexo.repository.ModuloRepository;
import com.corre_email.unexo.validation.DefinicionComando;
import com.corre_email.unexo.validation.Parametros;
import com.corre_email.unexo.validation.Reglas;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.corre_email.unexo.validation.ParamDef.obligatorio;
import static com.corre_email.unexo.validation.ParamDef.opcional;

/** CRUD de la tabla modulo: LISMDL, VERMDL, INSMDL, MODMDL, ELIMDL. */
@Configuration(proxyBeanMethods = false)
public class ModuloComandos {

    private static final String ENTIDAD = "el modulo";

    private final ModuloRepository repo;

    public ModuloComandos(ModuloRepository repo) {
        this.repo = repo;
    }

    // ---------- Definiciones ----------

    private static DefinicionComando conDatos(DefinicionComando definicion) {
        return definicion
                .param(obligatorio("codigo", Reglas.largo(1, 20)))
                .param(obligatorio("nombre", Reglas.largo(2, 100)))
                .param(opcional("descripcion", Reglas.largo(1, 255)))
                .param(opcional("requisitos", Reglas.largo(1, 255)))
                .param(opcional("temario", Reglas.largo(1, 255)))
                .param(obligatorio("horas", Reglas.numero(), Reglas.rango(1, 1000)))
                .param(obligatorio("precio", Reglas.decimal(), Reglas.rango(0, 1000000)))
                .param(obligatorio("se_oferta_por_separado", Reglas.unoDe("SI", "NO")))
                .param(obligatorio("estado_comercial", Reglas.unoDe("ACTIVO", "INACTIVO")));
    }

    // ---------- Comandos ----------

    @Bean
    public CommandStrategy lismdl() {
        return new Comando(Crud.listar("LISMDL", "Lista todos los modulos."),
                p -> Crud.listar("MODULOS", repo.findAll(), ModuloComandos::resumen));
    }

    @Bean
    public CommandStrategy vermdl() {
        return new Comando(Crud.porId("VERMDL", "Muestra un modulo por su id."),
                p -> detalle(Crud.buscar(repo, p.id("id"), ENTIDAD)));
    }

    @Bean
    public CommandStrategy insmdl() {
        return new Comando(conDatos(DefinicionComando.de("INSMDL", "Registra un nuevo modulo."))
                .ejemplo("INSMDL[\"JAVA-1\",\"Fundamentos de Java\",\"\",\"Logica basica\",\"POO, colecciones\",\"40\",\"400\",\"SI\",\"ACTIVO\"]"),
                this::insertar);
    }

    @Bean
    public CommandStrategy modmdl() {
        return new Comando(conDatos(DefinicionComando.de("MODMDL", "Modifica un modulo (el primer dato es su id).")
                .param(Crud.id("id")))
                .ejemplo("MODMDL[\"1\",\"JAVA-1\",\"Fundamentos de Java\",\"\",\"\",\"\",\"45\",\"450\",\"SI\",\"ACTIVO\"]"),
                this::modificar);
    }

    @Bean
    public CommandStrategy elimdl() {
        return new Comando(Crud.porId("ELIMDL", "Elimina un modulo por su id."),
                p -> Crud.eliminar(repo, p.id("id"), ENTIDAD));
    }

    // ---------- Logica ----------

    private String insertar(Parametros p) {
        String codigo = p.get("codigo");
        if (repo.existsByCodigoIgnoreCase(codigo)) {
            throw new UnexoException(ErrorCode.DATO_UNICO_REPETIDO, "Ya existe un modulo con codigo " + codigo + ".");
        }

        Modulo modulo = new Modulo();
        copiarDatos(p, modulo);
        repo.save(modulo);
        return "OK - Modulo registrado con id " + modulo.getId() + ".\n" + detalle(modulo);
    }

    private String modificar(Parametros p) {
        Long id = p.id("id");
        Modulo modulo = Crud.buscar(repo, id, ENTIDAD);
        String codigo = p.get("codigo");
        if (repo.existsByCodigoIgnoreCaseAndIdNot(codigo, id)) {
            throw new UnexoException(ErrorCode.DATO_UNICO_REPETIDO, "Ya existe otro modulo con codigo " + codigo + ".");
        }

        copiarDatos(p, modulo);
        repo.save(modulo);
        return "OK - Modulo modificado correctamente.\n" + detalle(modulo);
    }

    private static void copiarDatos(Parametros p, Modulo modulo) {
        modulo.setCodigo(p.get("codigo"));
        modulo.setNombre(p.get("nombre"));
        modulo.setDescripcion(p.opcional("descripcion"));
        modulo.setRequisitos(p.opcional("requisitos"));
        modulo.setTemario(p.opcional("temario"));
        modulo.setHoras(p.entero("horas"));
        modulo.setPrecio(p.decimal("precio"));
        modulo.setSeOfertaPorSeparado(p.siNo("se_oferta_por_separado"));
        modulo.setEstadoComercial(p.get("estado_comercial").equals("ACTIVO"));
    }

    // ---------- Formato ----------

    private static String resumen(Modulo x) {
        return "ID: " + x.getId() + " | " + x.getCodigo() + " | " + x.getNombre()
                + " | " + x.getHoras() + " h | Precio: " + Crud.texto(x.getPrecio())
                + " | " + Crud.activo(x.getEstadoComercial());
    }

    private static String detalle(Modulo x) {
        return "ID: " + x.getId()
                + "\nCodigo: " + x.getCodigo()
                + "\nNombre: " + x.getNombre()
                + "\nDescripcion: " + Crud.texto(x.getDescripcion())
                + "\nRequisitos: " + Crud.texto(x.getRequisitos())
                + "\nTemario: " + Crud.texto(x.getTemario())
                + "\nHoras: " + x.getHoras()
                + "\nPrecio: " + Crud.texto(x.getPrecio())
                + "\nSe oferta por separado: " + Crud.siNo(x.getSeOfertaPorSeparado())
                + "\nEstado comercial: " + Crud.activo(x.getEstadoComercial());
    }
}
