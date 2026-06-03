package fr.seynax.solvia.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "fr.seynax.solvia")
public class SolviaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SolviaBackendApplication.class, args);
    }
}
