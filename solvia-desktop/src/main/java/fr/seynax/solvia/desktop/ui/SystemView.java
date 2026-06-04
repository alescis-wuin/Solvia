package fr.seynax.solvia.desktop.ui;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

public final class SystemView extends VBox {

    public SystemView(Node backendStatusBanner) {
        getStyleClass().add("content-view");
        setSpacing(18);
        setPadding(new Insets(20));
        getChildren().addAll(
                new SectionCard("Système local", "État technique du backend local, de PostgreSQL et des actions de connexion.", backendStatusBanner),
                new SectionCard("Positionnement", Ui.help("Solvia reste local-first. Les contrôles système sont regroupés ici pour réduire le bruit visuel du dashboard."))
        );
    }
}
