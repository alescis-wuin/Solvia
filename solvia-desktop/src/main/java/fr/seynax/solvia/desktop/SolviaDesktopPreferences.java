package fr.seynax.solvia.desktop;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.prefs.Preferences;

public final class SolviaDesktopPreferences {

    public static final URI DEFAULT_BACKEND_URI = URI.create("http://127.0.0.1:8080");

    private static final String BACKEND_URL_KEY = "backendUrl";

    private final Preferences preferences = Preferences.userNodeForPackage(SolviaDesktopPreferences.class);

    public URI backendUri() {
        String configured = preferences.get(BACKEND_URL_KEY, DEFAULT_BACKEND_URI.toString());
        try {
            return normalizeBackendUri(configured);
        } catch (IllegalArgumentException exception) {
            return DEFAULT_BACKEND_URI;
        }
    }

    public URI saveBackendUri(String value) {
        URI uri = normalizeBackendUri(value);
        preferences.put(BACKEND_URL_KEY, uri.toString());
        return uri;
    }

    public static URI normalizeBackendUri(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("L’URL du backend est obligatoire.");
        }

        String normalized = value.strip();
        if (!normalized.contains("://")) {
            normalized = "http://" + normalized;
        }

        URI uri = URI.create(normalized);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!"http".equals(scheme) && !"https".equals(scheme)) {
            throw new IllegalArgumentException("L’URL du backend doit utiliser HTTP ou HTTPS.");
        }
        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("L’URL du backend doit contenir un hôte.");
        }

        try {
            return new URI(scheme, null, uri.getHost(), uri.getPort(), null, null, null);
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("URL backend invalide: " + value, exception);
        }
    }
}
