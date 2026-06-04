package fr.seynax.solvia.desktop.ui;

import java.net.URL;
import java.util.List;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.text.Font;

public final class ThemeSupport {

    private static final List<String> LUCIOLE_CANDIDATES = List.of(
            "/fonts/Luciole/Luciole-Regular.ttf",
            "/fonts/Luciole/Luciole-Regular.woff",
            "/fonts/Luciole/Luciole-Regular.woff2",
            "/fonts/Luciole/Luciole.ttf",
            "/fonts/Luciole/luciole.ttf"
    );

    private static final List<String> HACK_CANDIDATES = List.of(
            "/fonts/Hack/Hack-Regular.ttf",
            "/fonts/Hack/Hack-Regular.woff",
            "/fonts/Hack/Hack-Regular.woff2",
            "/fonts/Hack/Hack.ttf",
            "/fonts/Hack/hack.ttf"
    );

    private ThemeSupport() {
    }

    public static void install(Scene scene) {
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
        loadFirstAvailable(LUCIOLE_CANDIDATES);
        loadFirstAvailable(HACK_CANDIDATES);
        addStylesheet(scene, "/styles/solvia-dark.css");
        addStylesheet(scene, "/styles/solvia-dashboard.css");
    }

    private static void addStylesheet(Scene scene, String resourcePath) {
        URL stylesheet = ThemeSupport.class.getResource(resourcePath);
        if (stylesheet != null) {
            scene.getStylesheets().add(stylesheet.toExternalForm());
        }
    }

    private static void loadFirstAvailable(List<String> candidates) {
        for (String candidate : candidates) {
            URL resource = ThemeSupport.class.getResource(candidate);
            if (resource != null) {
                Font.loadFont(resource.toExternalForm(), 16);
                return;
            }
        }
    }
}
