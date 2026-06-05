package fr.seynax.solvia.desktop.ui;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class DashboardChartAxisSelector extends VBox {

    private final ToggleButton auto = new ToggleButton("Auto");
    private final ToggleButton manual = new ToggleButton("Manuel");
    private final DashboardDateTimeSelector timeMin = new DashboardDateTimeSelector("Début affiché");
    private final DashboardDateTimeSelector timeMax = new DashboardDateTimeSelector("Fin affichée");
    private final TextField timeStepAmount = field("1");
    private final ComboBox<String> timeStepUnit = new ComboBox<>();
    private final TextField valueMin = field("0");
    private final TextField valueMax = field("10000");
    private final TextField valueStep = field("500");
    private final Label dataHint = Ui.help("Aucune donnée de référence.");
    private final Label status = Ui.help("Mode automatique.");
    private final Button fitData = new Button("Cadrer les données");
    private final Button apply = new Button("Appliquer");
    private final Button reset = new Button("Réinitialiser");

    private AxisOverrides axisOverrides = AxisOverrides.auto();
    private DataBounds dataBounds;
    private Runnable onAxisChanged = () -> { };

    public DashboardChartAxisSelector() {
        getStyleClass().add("chart-axis-selector");
        setSpacing(12);

        ToggleGroup mode = new ToggleGroup();
        auto.setToggleGroup(mode);
        manual.setToggleGroup(mode);
        auto.setSelected(true);
        auto.getStyleClass().addAll("axis-mode-button", "axis-mode-auto");
        manual.getStyleClass().addAll("axis-mode-button", "axis-mode-manual");
        auto.setOnAction(event -> applyAuto());
        manual.setOnAction(event -> setManualControlsDisabled(false));

        timeStepUnit.getItems().setAll("s", "min", "h", "d");
        timeStepUnit.setValue("min");
        timeStepUnit.getStyleClass().add("axis-unit-box");

        fitData.setOnAction(event -> fitDataBounds());
        apply.setOnAction(event -> applyManual());
        reset.setOnAction(event -> reset());
        fitData.getStyleClass().add("axis-secondary-action");
        apply.getStyleClass().add("axis-primary-action");
        reset.getStyleClass().add("axis-secondary-action");

        HBox modeRow = new HBox(8, auto, manual);
        modeRow.getStyleClass().add("axis-mode-row");
        modeRow.setAlignment(Pos.CENTER_LEFT);

        HBox timeSelectors = new HBox(12, timeMin, timeMax);
        timeSelectors.getStyleClass().add("axis-date-time-row");
        timeSelectors.setAlignment(Pos.TOP_LEFT);

        GridPane numericGrid = new GridPane();
        numericGrid.getStyleClass().add("axis-grid");
        numericGrid.setHgap(10);
        numericGrid.setVgap(8);
        addRow(numericGrid, 0, "Pas horizontal", new HBox(8, timeStepAmount, timeStepUnit), "Pas vertical", valueStep);
        addRow(numericGrid, 1, "Valeur min", valueMin, "Valeur max", valueMax);

        HBox actions = new HBox(8, fitData, apply, reset);
        actions.getStyleClass().add("axis-actions");
        actions.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(header(), modeRow, dataHint, timeSelectors, numericGrid, actions, status);
        setManualControlsDisabled(true);
    }

    public void setOnAxisChanged(Runnable onAxisChanged) {
        this.onAxisChanged = onAxisChanged == null ? () -> { } : onAxisChanged;
    }

    public AxisOverrides axisOverrides() {
        return axisOverrides;
    }

    public void setDataBounds(DataBounds bounds) {
        dataBounds = bounds;
        if (bounds == null || !bounds.complete()) {
            dataHint.setText("Aucune donnée de référence.");
            timeMin.setPromptValue(null);
            timeMax.setPromptValue(null);
            return;
        }
        dataHint.setText("Données utiles : " + format(bounds.timeMin()) + " → " + format(bounds.timeMax())
                + " • " + format(bounds.valueMin()) + " → " + format(bounds.valueMax()));
        timeMin.setPromptValue(bounds.timeMin());
        timeMax.setPromptValue(bounds.timeMax());
        valueMin.setPromptText(format(bounds.valueMin()));
        valueMax.setPromptText(format(bounds.valueMax()));
        valueStep.setPromptText("Auto");
    }

    private Label header() {
        Label header = Ui.label("Pilotage des axes", "axis-selector-title");
        header.setAccessibleText("Sélecteur manuel ou automatique des axes du graphique.");
        return header;
    }

    private static TextField field(String prompt) {
        TextField field = new TextField();
        field.getStyleClass().add("axis-field");
        field.setPromptText(prompt);
        return field;
    }

    private void addRow(GridPane grid, int row, String leftLabel, javafx.scene.Node left, String rightLabel, javafx.scene.Node right) {
        grid.add(Ui.label(leftLabel, "axis-field-label"), 0, row);
        grid.add(left, 1, row);
        grid.add(Ui.label(rightLabel, "axis-field-label"), 2, row);
        grid.add(right, 3, row);
    }

    private void applyAuto() {
        axisOverrides = AxisOverrides.auto();
        status.setText("Mode automatique : Solvia choisit les bornes et les pas lisibles.");
        setManualControlsDisabled(true);
        onAxisChanged.run();
    }

    private void applyManual() {
        manual.setSelected(true);
        setManualControlsDisabled(false);
        try {
            LocalDateTime parsedTimeMin = timeMin.value();
            LocalDateTime parsedTimeMax = timeMax.value();
            if (parsedTimeMin != null && parsedTimeMax != null && !parsedTimeMin.isBefore(parsedTimeMax)) {
                throw new IllegalArgumentException("La date min doit être avant la date max.");
            }
            Duration parsedTimeStep = parseTimeStep();
            BigDecimal parsedValueMin = parseDecimal(valueMin, "valeur min");
            BigDecimal parsedValueMax = parseDecimal(valueMax, "valeur max");
            if (parsedValueMin != null && parsedValueMax != null && parsedValueMin.compareTo(parsedValueMax) >= 0) {
                throw new IllegalArgumentException("La valeur min doit être inférieure à la valeur max.");
            }
            BigDecimal parsedValueStep = parsePositiveDecimal(valueStep, "pas vertical");
            axisOverrides = new AxisOverrides(true, parsedTimeMin, parsedTimeMax, parsedTimeStep, parsedValueMin, parsedValueMax, parsedValueStep);
            status.setText("Réglage manuel appliqué. Les champs vides restent automatiques.");
            onAxisChanged.run();
        } catch (IllegalArgumentException exception) {
            status.setText(exception.getMessage());
        }
    }

    private void fitDataBounds() {
        if (dataBounds == null || !dataBounds.complete()) {
            status.setText("Aucune donnée disponible pour cadrer le graphique.");
            return;
        }
        manual.setSelected(true);
        setManualControlsDisabled(false);
        timeMin.setValue(dataBounds.timeMin());
        timeMax.setValue(dataBounds.timeMax());
        valueMin.setText(format(dataBounds.valueMin()));
        valueMax.setText(format(dataBounds.valueMax()));
        valueStep.clear();
        applyManual();
    }

    private void reset() {
        timeMin.clear();
        timeMax.clear();
        timeStepAmount.clear();
        timeStepUnit.setValue("min");
        valueMin.clear();
        valueMax.clear();
        valueStep.clear();
        auto.setSelected(true);
        applyAuto();
    }

    private void setManualControlsDisabled(boolean disabled) {
        timeMin.setDisable(disabled);
        timeMax.setDisable(disabled);
        timeStepAmount.setDisable(disabled);
        timeStepUnit.setDisable(disabled);
        valueMin.setDisable(disabled);
        valueMax.setDisable(disabled);
        valueStep.setDisable(disabled);
        fitData.setDisable(disabled);
        apply.setDisable(disabled);
    }

    private Duration parseTimeStep() {
        String amountText = timeStepAmount.getText();
        if (amountText == null || amountText.isBlank()) {
            return null;
        }
        long amount;
        try {
            amount = Long.parseLong(amountText.strip());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Pas horizontal invalide : utiliser un entier positif.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Le pas horizontal doit être strictement positif.");
        }
        return switch (timeStepUnit.getValue()) {
            case "s" -> Duration.ofSeconds(amount);
            case "min" -> Duration.ofMinutes(amount);
            case "h" -> Duration.ofHours(amount);
            case "d" -> Duration.ofDays(amount);
            default -> throw new IllegalArgumentException("Unité de pas horizontal invalide.");
        };
    }

    private BigDecimal parseDecimal(TextField field, String label) {
        String value = field.getText();
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.strip().replace(',', '.')).stripTrailingZeros();
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Nombre invalide pour " + label + ".");
        }
    }

    private BigDecimal parsePositiveDecimal(TextField field, String label) {
        BigDecimal value = parseDecimal(field, label);
        if (value == null) {
            return null;
        }
        if (value.signum() <= 0) {
            throw new IllegalArgumentException("Le " + label + " doit être strictement positif.");
        }
        return value;
    }

    private String format(LocalDateTime dateTime) {
        return dateTime == null ? "—" : DashboardDateTimeSelector.SUMMARY_FORMATTER.format(dateTime);
    }

    private String format(BigDecimal value) {
        return value == null ? "—" : value.stripTrailingZeros().toPlainString();
    }

    public record DataBounds(LocalDateTime timeMin, LocalDateTime timeMax, BigDecimal valueMin, BigDecimal valueMax) {
        boolean complete() {
            return timeMin != null && timeMax != null && valueMin != null && valueMax != null;
        }
    }

    public record AxisOverrides(
            boolean manual,
            LocalDateTime timeMin,
            LocalDateTime timeMax,
            Duration timeStep,
            BigDecimal valueMin,
            BigDecimal valueMax,
            BigDecimal valueStep
    ) {
        static AxisOverrides auto() {
            return new AxisOverrides(false, null, null, null, null, null, null);
        }
    }
}
