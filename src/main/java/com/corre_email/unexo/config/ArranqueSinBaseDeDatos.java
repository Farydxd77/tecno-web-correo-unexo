package com.corre_email.unexo.config;

import com.corre_email.unexo.service.Bitacora;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;

/**
 * Con ddl-auto=update Hibernate se conecta al arrancar para crear/actualizar las
 * tablas, y si la base de datos esta caida la app entera NO arranca. Esto prueba
 * la conexion antes: si no hay BD, arranca igual pero sin tocar las tablas, y cada
 * comando le respondera al usuario "No hay conexion con la base de datos por el
 * momento" hasta que vuelva.
 * <p>
 * Se registra en META-INF/spring.factories porque tiene que correr antes de que
 * Spring cree la conexion.
 */
public class ArranqueSinBaseDeDatos implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final int SEGUNDOS_DE_ESPERA = 3;

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment env = event.getEnvironment();
        String url = env.getProperty("spring.datasource.url");
        if (url == null || hayConexion(url, env.getProperty("spring.datasource.username"),
                env.getProperty("spring.datasource.password"))) {
            return;
        }

        Bitacora.sistema()
                .fila("Problema", "ERROR 501 - SIN CONEXION CON LA BASE DE DATOS\nLa base de datos no responde (" + url + ").")
                .fila("Que pasa", "La app arranca igual: a cada comando se le respondera que no hay conexion.\n"
                        + "Las tablas NO se actualizaron; si cambio el modelo, reinicie la app cuando la BD vuelva.")
                .imprimir();
        env.getPropertySources().addFirst(new MapPropertySource("sin-base-de-datos",
                Map.of("spring.jpa.hibernate.ddl-auto", "none")));
    }

    private static boolean hayConexion(String url, String usuario, String clave) {
        DriverManager.setLoginTimeout(SEGUNDOS_DE_ESPERA);
        try (Connection ignorada = DriverManager.getConnection(url, usuario, clave)) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
