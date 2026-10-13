package com.corre_email.unexo.repository;

import com.corre_email.unexo.model.ProgramaModulo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProgramaModuloRepository extends JpaRepository<ProgramaModulo, Long> {

    // "ProgramaId" = el campo programa y dentro de el su id (programa.id)
    boolean existsByProgramaIdAndModuloId(Long programaId, Long moduloId);

    boolean existsByProgramaIdAndModuloIdAndIdNot(Long programaId, Long moduloId, Long id);
}
