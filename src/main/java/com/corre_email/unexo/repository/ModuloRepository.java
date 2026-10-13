package com.corre_email.unexo.repository;

import com.corre_email.unexo.model.Modulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ModuloRepository extends JpaRepository<Modulo, Long> {

    boolean existsByCodigoIgnoreCase(String codigo);

    /** Para MODMDL: el codigo no puede repetirse con OTRO modulo. */
    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);
}
