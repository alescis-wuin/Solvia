package fr.seynax.solvia.desktop.ui;

import java.util.LinkedHashMap;
import java.util.Map;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public final class SolviaShell extends BorderPane {
    private final Label status = new Label("Ready");
    private final VBox top = new VBox(10);
    private final VBox navigation = new VBox(8);
    private final Map<String, Button> buttons = new LinkedHashMap<>();
    private final Map<String, Node> pages = new LinkedHashMap<>();

    public SolviaShell() {
        setTop(top);
        setLeft(sidebar());
        getStyleClass().add("solvia-shell");
    }

    public void addPage(String label, Node page) {
        Button button = new Button(iconLabel(label));
        button.setTooltip(new Tooltip(label));
        button.getStyleClass().add("rail-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER);
        button.setOnAction(event -> showPage(label));
        buttons.put(label, button);
        pages.put(label, page);
        navigation.getChildren().add(button);
        if (getCenter() == null) {
            showPage(label);
        }
    }

    public void showPage(String label) {
        Node page = pages.get(label);
        if (page == null) {
            return;
        }
        setCenter(page);
        buttons.forEach((buttonLabel, button) -> {
            boolean selected = buttonLabel.equals(label);
            button.setDisable(selected);
            button.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"), selected);
        });
    }

    public void setStatus(String text) {
        status.setText(text == null || text.isBlank() ? "Ready" : text);
    }

    public void setBackendStatusBanner(Node banner) {
        top.getChildren().clear();
        if (banner != null) {
            top.getChildren().add(banner);
        }
    }

    private Node sidebar() {
        Label appTitle = new Label("∿");
        appTitle.getStyleClass().add("rail-logo");
        navigation.setFillWidth(true);
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        Label avatar = new Label("SX");
        avatar.getStyleClass().add("rail-avatar");
        VBox sidebar = new VBox(16, appTitle, navigation, spacer, avatar);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setAlignment(Pos.TOP_CENTER);
        sidebar.setPadding(new Insets(18, 10, 18, 10));
        sidebar.setPrefWidth(76);
        sidebar.setMinWidth(76);
        sidebar.setMaxWidth(76);
        return sidebar;
    }

    private String iconLabel(String label) {
        return switch (label) {
            case "Dashboard" -> "▦";
            case "Comptes" -> "▭";
            case "Actifs & positions" -> "⌁";
            case "Saisie" -> "+";
            case "Système" -> "⚙";
            default -> label == null || label.isBlank() ? "·" : label.substring(0, 1).toUpperCase();
        };
    }
}
