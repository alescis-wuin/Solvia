package fr.seynax.solvia.desktop.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public final class MetricCard extends VBox {

    private final Label value = new Label("—");
    private final Label caption = Ui.help("—");

    public MetricCard(String title, String description) {
        getStyleClass().add("metric-card");
        setSpacing(6);
        setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = Ui.label(title, "metric-title");
        value.getStyleClass().add("metric-value");
        caption.getStyleClass().add("metric-caption");
        caption.setText(description == null || description.isBlank() ? "—" : description);
        getChildren().addAll(titleLabel, value, caption);
    }

    public void setValue(String text) {
        value.setText(text == null || text.isBlank() ? "—" : text);
    }

    public void setCaption(String text) {
        caption.setText(text == null || text.isBlank() ? "—" : text);
    }
}
