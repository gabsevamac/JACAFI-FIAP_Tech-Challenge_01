package com.jacafi.tech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.jacafi.tech.shared.time.ApplicationTimeZone;

@SpringBootApplication
@EnableScheduling
public class TechApplication {

    public static void main(String[] args) {
        // Antes de SpringApplication.run, e nao num @PostConstruct: assim nenhum bean chega a
        // ler o fuso default enquanto ele ainda e o da maquina.
        ApplicationTimeZone.enforceUtc();

        SpringApplication.run(TechApplication.class, args);
    }
}
