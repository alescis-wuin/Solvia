package fr.seynax.solvia.desktop.ui;

import java.time.LocalDate;

import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class DashboardFilters extends VBox {

    private final DatePicker from = Ui.tooltip(new DatePicker(LocalDate.now().minusDays(30)), "Date de début de la période.");
    private final DatePicker to = Ui.tooltip(new DatePicker(LocalDate.now()), "Date de fin de la période.");
    private final DashboardTimeStepSelector stepSelector = new DashboardTimeStepSelector();
    private final ComboBox<String> aggregation = Ui.tooltip(new ComboBox<>(), "Mode d'agrégation du graphique.");
    private final Button refresh = Ui.tooltip(new Button("↻"), "Recharge le dashboard.");

    public DashboardFilters() {
        getStyleClass().add("dashboard-filters");
        setSpacing(10);
        aggregation.getItems().setAll("last", "average");
        aggregation.setValue("last");
        refresh.getStyleClass().addAll("icon-button", "icon-refresh");
        refresh.setDisable(true);

        HBox row = Ui.style(new HBox(12,
                Ui.fieldLabel("Du", from), from,
                Ui.fieldLabel("Au", to), to,
                stepSelector,
                Ui.fieldLabel("Agrégation", aggregation), aggregation,
                refresh), "form-row", "dashboard-filter-row");
        getChildren().add(row);
    }

    public LocalDate from() {
        return from.getValue();
    }

    public LocalDate to() {
        return to.getValue();
    }

    public String bucket() {
        return stepSelector.apiBucket();
    }

    public String displayedBucket() {
        return stepSelector.displayCode();
    }

    public DashboardTimeStep selectedStep() {
        return stepSelector.selectedStep();
    }

    public String aggregation() {
        return aggregation.getValue();
    }

    public void onRefresh(Runnable action) {
        refresh.setOnAction(event -> action.run());
    }

    public void setRefreshDisabled(boolean disabled) {
        refresh.setDisable(disabled);
        stepSelector.setSelectorDisabled(disabled);
        aggregation.setDisable(disabled);
    }
}
