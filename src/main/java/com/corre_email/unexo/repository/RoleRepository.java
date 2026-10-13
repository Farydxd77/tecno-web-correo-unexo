package com.corre_email.unexo.repository;

import com.corre_email.unexo.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByNombreIgnoreCase(String nombre);

    /** Devuelve el rol con ese nombre; si todavia no existe en la tabla, lo crea. */
    default Role buscarOCrear(String nombre) {
        return findByNombreIgnoreCase(nombre).orElseGet(() -> save(new Role(nombre)));
    }
}
