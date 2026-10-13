package com.corre_email.unexo.model;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Inscripcion de un usuario en un grupo (Usuario -- 1..* Grupo). Es su propia
 * entidad y no un @ManyToMany porque guarda datos de la inscripcion.
 */
@Entity
@Table(name = "matricula", uniqueConstraints = @UniqueConstraint(columnNames = { "usuario_id", "grupo_id" }))
public class Matricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** PENDIENTE, ACTIVA o ANULADA */
    private String estado;
    private String origen;
    private Boolean aceptoRequisitos;
    private BigDecimal notaFinal;
    /** APROBADO o REPROBADO */
    private String resultado;

    // Apunta a persona.ci
    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Persona usuario;

    @ManyToOne
    @JoinColumn(name = "grupo_id")
    private Grupo grupo;

    public Matricula() {
    }

    public Matricula(Persona usuario, Grupo grupo, String estado, String origen, Boolean aceptoRequisitos) {
        this.usuario = usuario;
        this.grupo = grupo;
        this.estado = estado;
        this.origen = origen;
        this.aceptoRequisitos = aceptoRequisitos;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public Boolean getAceptoRequisitos() {
        return aceptoRequisitos;
    }

    public void setAceptoRequisitos(Boolean aceptoRequisitos) {
        this.aceptoRequisitos = aceptoRequisitos;
    }

    public BigDecimal getNotaFinal() {
        return notaFinal;
    }

    public void setNotaFinal(BigDecimal notaFinal) {
        this.notaFinal = notaFinal;
    }

    public String getResultado() {
        return resultado;
    }

    public void setResultado(String resultado) {
        this.resultado = resultado;
    }

    public Persona getUsuario() {
        return usuario;
    }

    public void setUsuario(Persona usuario) {
        this.usuario = usuario;
    }

    public Grupo getGrupo() {
        return grupo;
    }

    public void setGrupo(Grupo grupo) {
        this.grupo = grupo;
    }
}
