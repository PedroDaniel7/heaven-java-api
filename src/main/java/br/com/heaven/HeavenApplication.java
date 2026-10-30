package br.com.heaven;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Ponto de entrada da API REST do Heaven. */
@SpringBootApplication
public class HeavenApplication {
    public static void main(String[] args) {
        SpringApplication.run(HeavenApplication.class, args);
    }
}
