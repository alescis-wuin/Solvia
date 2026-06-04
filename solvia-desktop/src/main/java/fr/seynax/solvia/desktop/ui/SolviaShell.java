package fr.seynax.solvia.desktop.ui;

import java.util.LinkedHashMap;
import java.util.Map;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public final class SolviaShell extends BorderPane {
    private final Label title = new Label("Dashboard");
    private final Label status = new Label("Ready");
    private final VBox top = new VBox(10);
    private final VBox navigation = new VBox(8);
    private final Map<String, Button> buttons = new LinkedHashMap<>();
    private final Map<String, Node> pages = new LinkedHashMap<>();

    public SolviaShell() {
        top.getChildren().add(header());
        setTop(top);
        setLeft(sidebar());
        setBottom(footer());
        getStyleClass().add("solvia-shell");
    }

    public void addPage(String label, Node page) {
        Button button = new Button(label);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
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
        title.setText(label);
        setCenter(page);
        buttons.forEach((buttonLabel, button) -> button.setDisable(buttonLabel.equals(label)));
    }

    public void setStatus(String text) {
        status.setText(text == null || text.isBlank() ? "Ready" : text);
    }

    public void setBackendStatusBanner(Node banner) {
        if (top.getChildren().size() > 1) {
            top.getChildren().remove(1, top.getChildren().size());
        }
        if (banner != null) {
            top.getChildren().add(banner);
        }
    }

    private Node header() {
        title.getStyleClass().add("screen-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label backend = new Label("Local-first");
        backend.getStyleClass().add("help-text");
        HBox header = new HBox(16, title, spacer, backend);
        header.getStyleClass().add("header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 20, 12, 20));
        return header;
    }

    private Node sidebar() {
        Label appTitle = new Label("Solvia");
        appTitle.getStyleClass().add("app-title");
        navigation.setFillWidth(true);
        VBox sidebar = new VBox(16, appTitle, new Separator(), navigation);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(20, 12, 20, 12));
        sidebar.setPrefWidth(220);
        return sidebar;
    }

    private Node footer() {
        HBox footer = new HBox(status);
        footer.getStyleClass().add("footer");
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(8, 20, 12, 20));
        return footer;
    }
}
