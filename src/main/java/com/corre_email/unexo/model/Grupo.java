package com.corre_email.unexo.model;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "grupo")
public class Grupo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String version;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Integer cupoMinimo;
    private Integer cupoMaximo;
    private String enlaceMeet;

    /** ABIERTO, EN_CURSO o CERRADO */
    private String estado;

    // cohorte 1 -- 0..* grupo
    @ManyToOne
    @JoinColumn(name = "cohorte_id")
    private Cohorte cohorte;

    // Modulo 1 -- 0..* grupo: el modulo que se dicta en este grupo
    @ManyToOne
    @JoinColumn(name = "modulo_id")
    private Modulo modulo;

    // Usuario 1 -- 0..* grupo: el docente a cargo (apunta a persona.ci)
    @ManyToOne
    @JoinColumn(name = "docente_id")
    private Persona docente;

    public Grupo() {
    }

    public Grupo(String version, LocalDate fechaInicio, LocalDate fechaFin, Integer cupoMinimo, Integer cupoMaximo,
            String enlaceMeet, String estado, Cohorte cohorte, Modulo modulo, Persona docente) {
        this.version = version;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.cupoMinimo = cupoMinimo;
        this.cupoMaximo = cupoMaximo;
        this.enlaceMeet = enlaceMeet;
        this.estado = estado;
        this.cohorte = cohorte;
        this.modulo = modulo;
        this.docente = docente;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public Integer getCupoMinimo() {
        return cupoMinimo;
    }

    public void setCupoMinimo(Integer cupoMinimo) {
        this.cupoMinimo = cupoMinimo;
    }

    public Integer getCupoMaximo() {
        return cupoMaximo;
    }

    public void setCupoMaximo(Integer cupoMaximo) {
        this.cupoMaximo = cupoMaximo;
    }

    public String getEnlaceMeet() {
        return enlaceMeet;
    }

    public void setEnlaceMeet(String enlaceMeet) {
        this.enlaceMeet = enlaceMeet;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public Cohorte getCohorte() {
        return cohorte;
    }

    public void setCohorte(Cohorte cohorte) {
        this.cohorte = cohorte;
    }

    public Modulo getModulo() {
        return modulo;
    }

    public void setModulo(Modulo modulo) {
        this.modulo = modulo;
    }

    public Persona getDocente() {
        return docente;
    }

    public void setDocente(Persona docente) {
        this.docente = docente;
    }
}
