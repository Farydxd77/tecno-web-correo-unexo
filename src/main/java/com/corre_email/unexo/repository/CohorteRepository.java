package com.corre_email.unexo.repository;

import com.corre_email.unexo.model.Cohorte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CohorteRepository extends JpaRepository<Cohorte, Long> {
}
