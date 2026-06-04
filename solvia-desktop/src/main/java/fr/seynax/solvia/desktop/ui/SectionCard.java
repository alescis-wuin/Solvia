package fr.seynax.solvia.desktop.ui;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public final class SectionCard extends VBox {

    public SectionCard(String title, Node... content) {
        this(title, null, content);
    }

    public SectionCard(String title, String description, Node... content) {
        getStyleClass().add("section-card");
        setSpacing(12);
        Label titleLabel = Ui.sectionTitle(title);
        getChildren().add(titleLabel);
        if (description != null && !description.isBlank()) {
            getChildren().add(Ui.help(description));
        }
        if (content != null) {
            getChildren().addAll(content);
        }
    }
}
