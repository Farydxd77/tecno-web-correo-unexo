package com.corre_email.unexo.model;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "modulo")
public class Modulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String codigo;
    private String nombre;
    private String descripcion;
    private String requisitos;
    private String temario;
    private Integer horas;
    private BigDecimal precio;
    private Boolean seOfertaPorSeparado;
    private Boolean estadoComercial;

    public Modulo() {
    }

    public Modulo(String codigo, String nombre, String descripcion, String requisitos, String temario,
            Integer horas, BigDecimal precio, Boolean seOfertaPorSeparado, Boolean estadoComercial) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.requisitos = requisitos;
        this.temario = temario;
        this.horas = horas;
        this.precio = precio;
        this.seOfertaPorSeparado = seOfertaPorSeparado;
        this.estadoComercial = estadoComercial;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getRequisitos() {
        return requisitos;
    }

    public void setRequisitos(String requisitos) {
        this.requisitos = requisitos;
    }

    public String getTemario() {
        return temario;
    }

    public void setTemario(String temario) {
        this.temario = temario;
    }

    public Integer getHoras() {
        return horas;
    }

    public void setHoras(Integer horas) {
        this.horas = horas;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public Boolean getSeOfertaPorSeparado() {
        return seOfertaPorSeparado;
    }

    public void setSeOfertaPorSeparado(Boolean seOfertaPorSeparado) {
        this.seOfertaPorSeparado = seOfertaPorSeparado;
    }

    public Boolean getEstadoComercial() {
        return estadoComercial;
    }

    public void setEstadoComercial(Boolean estadoComercial) {
        this.estadoComercial = estadoComercial;
    }
}
