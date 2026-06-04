package fr.seynax.solvia.desktop.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public final class EmptyState extends VBox {

    private final Label icon = Ui.label("∅", "empty-icon");
    private final Label title = Ui.label("Aucune donnee", "empty-title");
    private final Label detail = Ui.help("Les donnees apparaitront ici apres la saisie.");

    public EmptyState() {
        getStyleClass().add("empty-state");
        setSpacing(8);
        setAlignment(Pos.CENTER);
        getChildren().addAll(icon, title, detail);
        hide();
    }

    public void show(String title, String detail) {
        this.title.setText(title == null || title.isBlank() ? "Aucune donnee" : title);
        this.detail.setText(detail == null || detail.isBlank() ? "Les donnees apparaitront ici apres la saisie." : detail);
        setVisible(true);
        setManaged(true);
    }

    public void hide() {
        setVisible(false);
        setManaged(false);
    }
}
