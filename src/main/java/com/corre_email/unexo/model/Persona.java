package com.corre_email.unexo.model;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "persona") // Nombre de la tabla en tu base de datos PostgreSQL
public class Persona {

    @Id
    private String ci; // O el identificador principal
    private String nombre;
    private String apellido;
    private String tipo;
    private String telefono;
    private String celular;
    private String email;
    private String correoInstitucional;

    private String password;
    private Boolean activo;

    // Relacion ManyToMany con Rol (tabla intermedia rol_usuario)
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "rol_usuario", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "rol_id"))
    private Set<Role> roles = new HashSet<>();

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    // Constructores, Getters y Setters
    public Persona() {
    }

    public Persona(String ci, String nombre, String apellido, String tipo, String telefono, String celular,
            String email) {
        this.ci = ci;
        this.nombre = nombre;
        this.apellido = apellido;
        this.tipo = tipo;
        this.telefono = telefono;
        this.celular = celular;
        this.email = email;
    }

    // Getters y Setters estándar...
    public String getCi() {
        return ci;
    }

    public void setCi(String ci) {
        this.ci = ci;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCelular() {
        return celular;
    }

    public void setCelular(String celular) {
        this.celular = celular;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCorreoInstitucional() {
        return correoInstitucional;
    }

    public void setCorreoInstitucional(String correoInstitucional) {
        this.correoInstitucional = correoInstitucional;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }
}