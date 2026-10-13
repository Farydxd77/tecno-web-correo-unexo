package com.corre_email.unexo.model;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "programa")
public class Programa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String codigo;
    private String nombre;
    private String descripcion;
    private Integer cantidadModulos;
    private BigDecimal precioContado;
    private Boolean estadoComercial;

    // JPA necesita el constructor vacio para crear el objeto al leer de la BD
    public Programa() {
    }

    public Programa(String codigo, String nombre, String descripcion, Integer cantidadModulos,
            BigDecimal precioContado, Boolean estadoComercial) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.cantidadModulos = cantidadModulos;
        this.precioContado = precioContado;
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

    public Integer getCantidadModulos() {
        return cantidadModulos;
    }

    public void setCantidadModulos(Integer cantidadModulos) {
        this.cantidadModulos = cantidadModulos;
    }

    public BigDecimal getPrecioContado() {
        return precioContado;
    }

    public void setPrecioContado(BigDecimal precioContado) {
        this.precioContado = precioContado;
    }

    public Boolean getEstadoComercial() {
        return estadoComercial;
    }

    public void setEstadoComercial(Boolean estadoComercial) {
        this.estadoComercial = estadoComercial;
    }
}
