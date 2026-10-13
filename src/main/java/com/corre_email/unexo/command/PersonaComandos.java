package com.corre_email.unexo.command;

import com.corre_email.unexo.config.RolesIniciales;
import com.corre_email.unexo.exception.ErrorCode;
import com.corre_email.unexo.exception.UnexoException;
import com.corre_email.unexo.model.Persona;
import com.corre_email.unexo.model.Role;
import com.corre_email.unexo.repository.PersonaRepository;
import com.corre_email.unexo.repository.RoleRepository;
import com.corre_email.unexo.validation.DefinicionComando;
import com.corre_email.unexo.validation.ParamDef;
import com.corre_email.unexo.validation.Parametros;
import com.corre_email.unexo.validation.Reglas;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.corre_email.unexo.validation.ParamDef.obligatorio;
import static com.corre_email.unexo.validation.ParamDef.opcional;

/**
 * CRUD de la tabla persona: LISPER, VERPER, INSPER, MODPER, ELIPER.
 * Cada metodo @Bean es un comando; Spring los junta con los demas y el
 * CommandDispatcher los encuentra por su nombre.
 */
@Configuration(proxyBeanMethods = false)
public class PersonaComandos {

    private static final String ENTIDAD = "la persona";

    private final PersonaRepository repo;
    private final RoleRepository roles;

    public PersonaComandos(PersonaRepository repo, RoleRepository roles) {
        this.repo = repo;
        this.roles = roles;
    }

    // ---------- Definiciones ----------

    private static ParamDef ci() {
        return obligatorio("ci", Reglas.numero(), Reglas.largo(5, 10));
    }

    /** Los datos de la persona, en el mismo orden para INSPER y MODPER (el CI va primero en los dos). */
    private static DefinicionComando conDatos(DefinicionComando definicion) {
        return definicion
                .param(ci())
                .param(obligatorio("nombre", Reglas.soloLetras(), Reglas.largo(2, 50)))
                .param(obligatorio("apellido", Reglas.soloLetras(), Reglas.largo(2, 50)))
                .param(obligatorio("rol", Reglas.unoDe(RolesIniciales.TODOS.toArray(String[]::new))))
                .param(opcional("telefono", Reglas.numero(), Reglas.largo(7, 8)))
                .param(obligatorio("celular", Reglas.numero(), Reglas.largo(8, 8)))
                .param(obligatorio("email", Reglas.email(), Reglas.largo(5, 100)))
                .param(opcional("correo_institucional", Reglas.email(), Reglas.largo(5, 100)));
    }

    // ---------- Comandos ----------

    @Bean
    public CommandStrategy lisper() {
        return new Comando(Crud.listar("LISPER", "Lista todas las personas."),
                p -> Crud.listar("PERSONAS", repo.findAll(), PersonaComandos::resumen));
    }

    @Bean
    public CommandStrategy verper() {
        return new Comando(DefinicionComando.de("VERPER", "Muestra una persona por su CI.")
                .param(ci())
                .ejemplo("VERPER[\"12345678\"]"),
                p -> detalle(Crud.buscar(repo, p.get("ci"), ENTIDAD)));
    }

    @Bean
    public CommandStrategy insper() {
        return new Comando(conDatos(DefinicionComando.de("INSPER", "Registra una nueva persona."))
                .ejemplo("INSPER[\"12345678\",\"Juan\",\"Perez\",\"Estudiante\",\"3312345\",\"70011223\",\"juan@tecnoweb.org.bo\"]"),
                this::insertar);
    }

    @Bean
    public CommandStrategy modper() {
        return new Comando(conDatos(DefinicionComando.de("MODPER", "Modifica los datos de una persona (se busca por CI)."))
                .ejemplo("MODPER[\"12345678\",\"Juan\",\"Perez\",\"Docente\",\"\",\"70011223\",\"juan@tecnoweb.org.bo\"]"),
                this::modificar);
    }

    @Bean
    public CommandStrategy eliper() {
        return new Comando(DefinicionComando.de("ELIPER", "Elimina una persona por su CI.")
                .param(ci())
                .ejemplo("ELIPER[\"12345678\"]"),
                p -> Crud.eliminar(repo, p.get("ci"), ENTIDAD));
    }

    // ---------- Logica ----------

    private String insertar(Parametros p) {
        String ci = p.get("ci");
        String email = p.get("email");
        if (repo.existsById(ci)) {
            throw new UnexoException(ErrorCode.REGISTRO_DUPLICADO, "Ya existe una persona con CI " + ci + ".");
        }
        if (repo.existsByEmailIgnoreCase(email)) {
            throw new UnexoException(ErrorCode.DATO_UNICO_REPETIDO,
                    "El email " + email + " ya esta registrado para otra persona.");
        }

        Persona persona = new Persona(ci, p.get("nombre"), p.get("apellido"), p.get("rol"),
                p.opcional("telefono"), p.get("celular"), email);
        persona.setCorreoInstitucional(p.opcional("correo_institucional"));
        persona.setActivo(true);
        asignarRol(persona, p.get("rol"));
        repo.save(persona);
        return "OK - Persona registrada correctamente.\n" + detalle(persona);
    }

    private String modificar(Parametros p) {
        String ci = p.get("ci");
        String email = p.get("email");
        Persona persona = Crud.buscar(repo, ci, ENTIDAD);
        if (repo.existsByEmailIgnoreCaseAndCiNot(email, ci)) {
            throw new UnexoException(ErrorCode.DATO_UNICO_REPETIDO,
                    "El email " + email + " ya esta registrado para otra persona.");
        }

        persona.setNombre(p.get("nombre"));
        persona.setApellido(p.get("apellido"));
        persona.setTipo(p.get("rol"));
        asignarRol(persona, p.get("rol"));
        persona.setTelefono(p.opcional("telefono"));
        persona.setCelular(p.get("celular"));
        persona.setEmail(email);
        persona.setCorreoInstitucional(p.opcional("correo_institucional"));
        repo.save(persona);
        return "OK - Persona modificada correctamente.\n" + detalle(persona);
    }

    /** Cada persona tiene un solo rol: el nuevo reemplaza al anterior. */
    private void asignarRol(Persona persona, String nombreRol) {
        Set<Role> nuevos = new HashSet<>();
        nuevos.add(roles.buscarOCrear(nombreRol));
        persona.setRoles(nuevos);
    }

    // ---------- Formato ----------

    private static String nombresDeRoles(Persona x) {
        if (x.getRoles() == null || x.getRoles().isEmpty()) {
            return "-";
        }
        return x.getRoles().stream().map(Role::getNombre).sorted().collect(Collectors.joining(", "));
    }

    private static String resumen(Persona x) {
        return "CI: " + x.getCi() + " | " + x.getNombre() + " " + x.getApellido()
                + " | " + x.getTipo() + " | " + x.getEmail();
    }

    private static String detalle(Persona x) {
        return "CI: " + x.getCi()
                + "\nNombre: " + x.getNombre() + " " + x.getApellido()
                + "\nRol: " + nombresDeRoles(x)
                + "\nTelefono: " + Crud.texto(x.getTelefono())
                + "\nCelular: " + x.getCelular()
                + "\nEmail: " + x.getEmail()
                + "\nCorreo institucional: " + Crud.texto(x.getCorreoInstitucional())
                + "\nActivo: " + Crud.siNo(x.getActivo());
    }
}
