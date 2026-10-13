package com.corre_email.unexo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // <-- ¡Agrega esta línea!
public class UnexoApplication {

	public static void main(String[] args) {
		SpringApplication.run(UnexoApplication.class, args);
	}
}