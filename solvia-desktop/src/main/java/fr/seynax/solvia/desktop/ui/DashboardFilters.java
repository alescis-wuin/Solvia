package fr.seynax.solvia.desktop.ui;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class DashboardFilters extends VBox {

    private final DatePicker from = Ui.tooltip(new DatePicker(LocalDate.now().minusDays(30)), "Date de début de la période.");
    private final DatePicker to = Ui.tooltip(new DatePicker(LocalDate.now()), "Date de fin de la période.");
    private final DashboardTimeStepSelector stepSelector = new DashboardTimeStepSelector();
    private final ComboBox<String> aggregation = Ui.tooltip(new ComboBox<>(), "Mode d'agrégation du graphique.");
    private final Button refresh = Ui.tooltip(new Button("↻"), "Recharge le dashboard.");
    private final ToggleGroup rangeGroup = new ToggleGroup();
    private final List<ToggleButton> rangeButtons = new ArrayList<>();
    private Runnable refreshAction = () -> { };
    private boolean refreshDisabled = true;
    private boolean internalChange;

    public DashboardFilters() {
        getStyleClass().add("dashboard-filters");
        setSpacing(12);
        aggregation.getItems().setAll("last", "average");
        aggregation.setValue("last");
        refresh.getStyleClass().addAll("icon-button", "icon-refresh");
        refresh.setDisable(true);

        stepSelector.onChanged(this::autoRefresh);
        from.valueProperty().addListener((observable, previous, value) -> {
            if (!internalChange) {
                clearRangeSelection();
            }
            autoRefresh();
        });
        to.valueProperty().addListener((observable, previous, value) -> {
            if (!internalChange) {
                clearRangeSelection();
            }
            autoRefresh();
        });
        aggregation.valueProperty().addListener((observable, previous, value) -> autoRefresh());
        rangeGroup.selectedToggleProperty().addListener((observable, previous, selected) -> applySelectedRange(selected));

        HBox ranges = Ui.style(new HBox(4), "dashboard-range-row");
        for (DashboardRange range : DashboardRange.values()) {
            ToggleButton button = Ui.tooltip(new ToggleButton(range.label()), "Afficher la période " + range.label() + ".");
            button.getStyleClass().add("range-button");
            button.setUserData(range);
            button.setToggleGroup(rangeGroup);
            rangeButtons.add(button);
            ranges.getChildren().add(button);
            if (range == DashboardRange.QUARTER) {
                button.setSelected(true);
            }
        }

        HBox dates = Ui.style(new HBox(12,
                Ui.fieldLabel("Du", from), from,
                Ui.fieldLabel("Au", to), to,
                Ui.fieldLabel("Agrégation", aggregation), aggregation,
                refresh), "form-row", "dashboard-filter-row");
        getChildren().addAll(ranges, stepSelector, dates);
        applyRange(DashboardRange.QUARTER, true);
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
        refreshAction = action == null ? () -> { } : action;
        refresh.setOnAction(event -> refreshAction.run());
    }

    public void setRefreshDisabled(boolean disabled) {
        refreshDisabled = disabled;
        refresh.setDisable(disabled);
        stepSelector.setSelectorDisabled(disabled);
        aggregation.setDisable(disabled);
        rangeButtons.forEach(button -> button.setDisable(disabled));
    }

    private void applySelectedRange(Toggle selected) {
        if (internalChange || selected == null || !(selected.getUserData() instanceof DashboardRange range)) {
            return;
        }
        applyRange(range, false);
        autoRefresh();
    }

    private void applyRange(DashboardRange range, boolean silent) {
        if (range == null) {
            return;
        }
        internalChange = true;
        try {
            LocalDate end = LocalDate.now();
            to.setValue(end);
            from.setValue(range.startDate(end));
            stepSelector.selectCodeSilently(range.stepCode());
        } finally {
            internalChange = false;
        }
        if (!silent) {
            autoRefresh();
        }
    }

    private void clearRangeSelection() {
        internalChange = true;
        try {
            rangeGroup.selectToggle(null);
        } finally {
            internalChange = false;
        }
    }

    private void autoRefresh() {
        if (!refreshDisabled && !internalChange) {
            refreshAction.run();
        }
    }
}
