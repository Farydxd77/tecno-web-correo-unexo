package com.corre_email.unexo.config;

import com.corre_email.unexo.repository.RoleRepository;
import com.corre_email.unexo.service.Bitacora;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Al arrancar deja creados en la tabla rol los roles del sistema (solo los que
 * falten). Son los mismos valores que acepta el campo "rol" de INSPER/MODPER.
 */
@Component
public class RolesIniciales implements ApplicationRunner {

    public static final String ADMINISTRADOR = "Administrador";
    public static final String DOCENTE = "Docente";
    public static final String ESTUDIANTE = "Estudiante";
    public static final List<String> TODOS = List.of(ADMINISTRADOR, DOCENTE, ESTUDIANTE);

    private final RoleRepository repo;

    public RolesIniciales(RoleRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            TODOS.forEach(repo::buscarOCrear);
        } catch (Exception e) {
            // Sin BD la app arranca igual (ver ArranqueSinBaseDeDatos); INSPER crea
            // el rol que falte cuando la conexion vuelva.
            Bitacora.sistema()
                    .fila("Problema", "No se pudieron crear los roles iniciales.")
                    .fila("Detalle", e.getMessage())
                    .imprimir();
        }
    }
}
