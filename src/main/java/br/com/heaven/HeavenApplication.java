package br.com.heaven;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Ponto de entrada da API REST do Heaven. O app de console (br.com.heaven.app.Main) continua existindo. */
@SpringBootApplication
public class HeavenApplication {
    public static void main(String[] args) {
        SpringApplication.run(HeavenApplication.class, args);
    }
}
