package fr.seynax.solvia.desktop.ui;

import java.time.LocalDate;

import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class DashboardFilters extends VBox {

    private final DatePicker from = Ui.tooltip(new DatePicker(LocalDate.now().minusDays(30)), "Date de debut de la periode.");
    private final DatePicker to = Ui.tooltip(new DatePicker(LocalDate.now()), "Date de fin de la periode.");
    private final ComboBox<String> bucket = Ui.tooltip(new ComboBox<>(), "Pas d'agregation de la serie temporelle.");
    private final ComboBox<String> aggregation = Ui.tooltip(new ComboBox<>(), "Mode d'agregation du graphique.");
    private final Button refresh = Ui.tooltip(new Button("Actualiser"), "Recharge le dashboard.");

    public DashboardFilters() {
        getStyleClass().add("section-card");
        setSpacing(12);
        bucket.getItems().setAll("1d", "2d", "1w", "1m");
        bucket.setValue("2d");
        aggregation.getItems().setAll("last", "average");
        aggregation.setValue("last");
        refresh.setDisable(true);

        HBox row = Ui.style(new HBox(12,
                Ui.fieldLabel("Du", from), from,
                Ui.fieldLabel("Au", to), to,
                Ui.fieldLabel("Pas", bucket), bucket,
                Ui.fieldLabel("Agregation", aggregation), aggregation,
                refresh), "form-row");
        getChildren().addAll(
                Ui.sectionTitle("Filtres"),
                Ui.help("Selectionne la periode et la granularite du graphique."),
                row
        );
    }

    public LocalDate from() {
        return from.getValue();
    }

    public LocalDate to() {
        return to.getValue();
    }

    public String bucket() {
        return bucket.getValue();
    }

    public String aggregation() {
        return aggregation.getValue();
    }

    public void onRefresh(Runnable action) {
        refresh.setOnAction(event -> action.run());
    }

    public void setRefreshDisabled(boolean disabled) {
        refresh.setDisable(disabled);
    }
}
