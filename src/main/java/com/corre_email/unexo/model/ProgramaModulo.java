package com.corre_email.unexo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Tabla intermedia programa 1..* -- 1..* Modulo. No se usa @ManyToMany (como en
 * rol_usuario) porque ademas guarda el orden del modulo dentro del programa.
 */
@Entity
@Table(name = "programa_modulo", uniqueConstraints = @UniqueConstraint(columnNames = { "programa_id", "modulo_id" }))
public class ProgramaModulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "programa_id")
    private Programa programa;

    @ManyToOne
    @JoinColumn(name = "modulo_id")
    private Modulo modulo;

    private Integer orden;

    public ProgramaModulo() {
    }

    public ProgramaModulo(Programa programa, Modulo modulo, Integer orden) {
        this.programa = programa;
        this.modulo = modulo;
        this.orden = orden;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Programa getPrograma() {
        return programa;
    }

    public void setPrograma(Programa programa) {
        this.programa = programa;
    }

    public Modulo getModulo() {
        return modulo;
    }

    public void setModulo(Modulo modulo) {
        this.modulo = modulo;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }
}
