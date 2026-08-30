package com.jacafi.tech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.jacafi.tech.shared.time.ApplicationTimeZone;

@SpringBootApplication
@EnableScheduling
public class TechApplication {

    public static void main(String[] args) {

        ApplicationTimeZone.enforceUtc();

        SpringApplication.run(TechApplication.class, args);
    }
}
