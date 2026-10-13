package com.corre_email.unexo.repository;

import com.corre_email.unexo.model.Programa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProgramaRepository extends JpaRepository<Programa, Long> {

    boolean existsByCodigoIgnoreCase(String codigo);

    /** Para MODPRG: el codigo no puede repetirse con OTRO programa. */
    boolean existsByCodigoIgnoreCaseAndIdNot(String codigo, Long id);
}
