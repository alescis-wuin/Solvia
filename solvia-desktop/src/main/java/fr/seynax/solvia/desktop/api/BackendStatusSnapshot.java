package fr.seynax.solvia.desktop.api;

import java.time.Instant;
import java.util.Objects;

import fr.seynax.solvia.desktop.api.ApiDtos.ReadinessDto;

public record BackendStatusSnapshot(
        BackendConnectionState state,
        String baseUrl,
        String service,
        String backendStatus,
        String databaseStatus,
        Instant checkedAt,
        String message,
        String technicalMessage
) {

    public BackendStatusSnapshot {
        state = Objects.requireNonNull(state, "backend state is required");
        baseUrl = baseUrl == null || baseUrl.isBlank() ? "http://127.0.0.1:8080" : baseUrl.strip();
        message = message == null || message.isBlank() ? defaultMessage(state) : message.strip();
        technicalMessage = technicalMessage == null || technicalMessage.isBlank() ? null : technicalMessage.strip();
    }

    public static BackendStatusSnapshot checking(String baseUrl) {
        return new BackendStatusSnapshot(
                BackendConnectionState.CHECKING,
                baseUrl,
                null,
                null,
                null,
                Instant.now(),
                "Vérification du backend local...",
                null
        );
    }

    public static BackendStatusSnapshot from(String baseUrl, ReadinessDto readiness) {
        BackendConnectionState state = switch (normalize(readiness.status())) {
            case "UP" -> BackendConnectionState.CONNECTED;
            case "DEGRADED" -> BackendConnectionState.DEGRADED;
            default -> BackendConnectionState.ERROR;
        };
        return new BackendStatusSnapshot(
                state,
                baseUrl,
                readiness.service(),
                readiness.backendStatus(),
                readiness.databaseStatus(),
                readiness.checkedAt(),
                readiness.message(),
                null
        );
    }

    public static BackendStatusSnapshot unavailable(String baseUrl, String technicalMessage) {
        return new BackendStatusSnapshot(
                BackendConnectionState.UNAVAILABLE,
                baseUrl,
                null,
                "DOWN",
                "UNKNOWN",
                Instant.now(),
                "Backend local indisponible. Démarrer le backend ou vérifier l’URL configurée.",
                technicalMessage
        );
    }

    public static BackendStatusSnapshot error(String baseUrl, String message, String technicalMessage) {
        return new BackendStatusSnapshot(
                BackendConnectionState.ERROR,
                baseUrl,
                null,
                "UNKNOWN",
                "UNKNOWN",
                Instant.now(),
                message,
                technicalMessage
        );
    }

    public boolean canLoadData() {
        return state == BackendConnectionState.CONNECTED;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip().toUpperCase(java.util.Locale.ROOT);
    }

    private static String defaultMessage(BackendConnectionState state) {
        return switch (state) {
            case CHECKING -> "Vérification du backend local...";
            case CONNECTED -> "Backend local connecté.";
            case DEGRADED -> "Backend local partiellement disponible.";
            case UNAVAILABLE -> "Backend local indisponible.";
            case ERROR -> "Erreur backend.";
        };
    }
}
