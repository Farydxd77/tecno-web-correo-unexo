package com.corre_email.unexo.repository;

import com.corre_email.unexo.model.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonaRepository extends JpaRepository<Persona, String> {
    // Al extender de JpaRepository, Spring Boot te regala automáticamente
    // los métodos para guardar, buscar, listar y eliminar sin que tengas que
    // escribir SQL.

    // Spring arma la consulta a partir del nombre del metodo
    boolean existsByEmailIgnoreCase(String email);

    /** Para MODPER: el email no puede estar usado por OTRA persona. */
    boolean existsByEmailIgnoreCaseAndCiNot(String email, String ci);
}
