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
    private final VBox navigation = new VBox(8);
    private final Map<String, Button> buttons = new LinkedHashMap<>();

    public SolviaShell() {
        setTop(header());
        setLeft(sidebar());
        setBottom(footer());
    }

    public void addPage(String label, Node page) {
        Button button = new Button(label);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setOnAction(event -> show(label, page));
        buttons.put(label, button);
        navigation.getChildren().add(button);
        if (getCenter() == null) {
            show(label, page);
        }
    }

    public void setStatus(String text) {
        status.setText(text == null || text.isBlank() ? "Ready" : text);
    }

    private void show(String label, Node page) {
        title.setText(label);
        setCenter(page);
    }

    private Node header() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(16, title, spacer, new Label("Local backend"));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 20, 12, 20));
        return header;
    }

    private Node sidebar() {
        Label appTitle = new Label("Solvia");
        navigation.setFillWidth(true);
        VBox sidebar = new VBox(16, appTitle, new Separator(), navigation);
        sidebar.setPadding(new Insets(20, 12, 20, 12));
        sidebar.setPrefWidth(220);
        return sidebar;
    }

    private Node footer() {
        HBox footer = new HBox(status);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setPadding(new Insets(8, 20, 12, 20));
        return footer;
    }
}
