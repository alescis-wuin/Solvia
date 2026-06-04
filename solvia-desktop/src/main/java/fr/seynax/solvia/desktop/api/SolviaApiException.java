package fr.seynax.solvia.desktop.api;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class SolviaApiException extends RuntimeException {

    private final int statusCode;
    private final String userMessage;
    private final String responseBody;

    private SolviaApiException(int statusCode, String userMessage, String responseBody) {
        super("Backend returned HTTP " + statusCode + ": " + userMessage);
        this.statusCode = statusCode;
        this.userMessage = userMessage;
        this.responseBody = responseBody;
    }

    public static SolviaApiException from(int statusCode, String body, ObjectMapper objectMapper) {
        return new SolviaApiException(statusCode, extractMessage(statusCode, body, objectMapper), body);
    }

    public int statusCode() {
        return statusCode;
    }

    public String userMessage() {
        return userMessage;
    }

    public String responseBody() {
        return responseBody;
    }

    private static String extractMessage(int statusCode, String body, ObjectMapper objectMapper) {
        if (body == null || body.isBlank()) {
            return fallback(statusCode);
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            String message = text(root, "message");
            String fields = fieldErrors(root.path("fieldErrors"));
            if (!fields.isBlank()) {
                return message == null || message.isBlank() ? fields : message + " — " + fields;
            }
            if (message != null && !message.isBlank()) {
                return message;
            }
            String error = text(root, "error");
            if (error != null && !error.isBlank()) {
                return error;
            }
        } catch (IOException exception) {
            return trim(body);
        }
        return fallback(statusCode);
    }

    private static String text(JsonNode root, String field) {
        JsonNode value = root.path(field);
        return value.isTextual() ? value.asText() : null;
    }

    private static String fieldErrors(JsonNode fieldErrors) {
        if (!fieldErrors.isObject() || fieldErrors.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        Iterator<Map.Entry<String, JsonNode>> iterator = fieldErrors.fields();
        while (iterator.hasNext()) {
            Map.Entry<String, JsonNode> entry = iterator.next();
            if (!builder.isEmpty()) {
                builder.append("; ");
            }
            builder.append(entry.getKey()).append(": ").append(entry.getValue().asText());
        }
        return builder.toString();
    }

    private static String trim(String value) {
        return value.length() > 240 ? value.substring(0, 237) + "..." : value;
    }

    private static String fallback(int statusCode) {
        return switch (statusCode) {
            case 400 -> "Requête invalide.";
            case 404 -> "Ressource introuvable.";
            case 503 -> "Service indisponible.";
            default -> "Erreur backend HTTP " + statusCode + ".";
        };
    }
}
