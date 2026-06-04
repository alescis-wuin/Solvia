package fr.seynax.solvia.desktop.ui;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public final class StateMessage extends VBox {

    private final Label title = Ui.label("—", "state-title");
    private final Label detail = Ui.help("—");

    public StateMessage() {
        getStyleClass().add("state-message");
        setSpacing(6);
        getChildren().addAll(title, detail);
        setVisible(false);
        setManaged(false);
    }

    public void show(String title, String detail, String styleClass) {
        this.title.setText(title == null || title.isBlank() ? "Information" : title);
        this.detail.setText(detail == null || detail.isBlank() ? "—" : detail);
        getStyleClass().removeAll("state-info", "state-warning", "state-error", "state-success");
        if (styleClass != null && !styleClass.isBlank()) {
            getStyleClass().add(styleClass);
        }
        setVisible(true);
        setManaged(true);
    }

    public void hide() {
        setVisible(false);
        setManaged(false);
    }
}
