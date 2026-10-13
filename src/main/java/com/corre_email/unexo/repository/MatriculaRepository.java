package com.corre_email.unexo.repository;

import com.corre_email.unexo.model.Matricula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MatriculaRepository extends JpaRepository<Matricula, Long> {

    // "UsuarioCi" = el campo usuario y dentro de el su ci (usuario.ci)
    boolean existsByUsuarioCiAndGrupoId(String ci, Long grupoId);

    boolean existsByUsuarioCiAndGrupoIdAndIdNot(String ci, Long grupoId, Long id);

    /** Cupos ocupados: las matriculas del grupo que no estan anuladas. */
    long countByGrupoIdAndEstadoNot(Long grupoId, String estado);

    long countByGrupoIdAndEstadoNotAndIdNot(Long grupoId, String estado, Long id);
}
