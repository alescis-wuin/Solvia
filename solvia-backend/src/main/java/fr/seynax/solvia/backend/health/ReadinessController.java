package fr.seynax.solvia.backend.health;

import java.sql.Connection;
import java.time.Instant;

import javax.sql.DataSource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ReadinessController {

    private static final String STATUS_UP = "UP";
    private static final String STATUS_DEGRADED = "DEGRADED";
    private static final String STATUS_DOWN = "DOWN";

    private final DataSource dataSource;

    ReadinessController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/api/readiness")
    ReadinessResponse readiness() {
        ComponentState database = databaseState();
        String status = STATUS_UP.equals(database.status()) ? STATUS_UP : STATUS_DEGRADED;
        String message = STATUS_UP.equals(database.status())
                ? "Backend and PostgreSQL are reachable."
                : "Backend is running but PostgreSQL is not reachable: " + database.message();

        return new ReadinessResponse(
                status,
                "solvia-backend",
                Instant.now(),
                STATUS_UP,
                database.status(),
                message
        );
    }

    private ComponentState databaseState() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(2)) {
                return ComponentState.up("PostgreSQL connection validated.");
            }
            return ComponentState.down("JDBC connection validation failed.");
        } catch (Exception exception) {
            return ComponentState.down(safeMessage(exception));
        }
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        String firstLine = message.lines().findFirst().orElse(exception.getClass().getSimpleName()).strip();
        if (firstLine.length() > 240) {
            return firstLine.substring(0, 237) + "...";
        }
        return firstLine;
    }

    record ReadinessResponse(
            String status,
            String service,
            Instant checkedAt,
            String backendStatus,
            String databaseStatus,
            String message
    ) {
    }

    private record ComponentState(String status, String message) {
        static ComponentState up(String message) {
            return new ComponentState(STATUS_UP, message);
        }

        static ComponentState down(String message) {
            return new ComponentState(STATUS_DOWN, message);
        }
    }
}