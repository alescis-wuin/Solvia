package fr.seynax.solvia.backend.health;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ReadinessController {

    @GetMapping("/api/readiness")
    Map<String, String> readiness() {
        return Map.of(
                "status", "UP",
                "service", "solvia-backend",
                "checkedAt", Instant.now().toString()
        );
    }
}
