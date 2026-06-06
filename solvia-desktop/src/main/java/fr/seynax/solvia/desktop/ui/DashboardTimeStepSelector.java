package fr.seynax.solvia.desktop.ui;

import java.util.List;
import java.util.Objects;

import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class DashboardTimeStepSelector extends VBox {

    private final ComboBox<DashboardTimeStep> selector = Ui.tooltip(new ComboBox<>(), "Sélectionne le pas de lecture du graphique.");
    private final Label selected = Ui.label("—", "monospace");
    private final Label detail = Ui.help("—");
    private Runnable onChanged = () -> { };
    private boolean silentSelection;

    public DashboardTimeStepSelector() {
        getStyleClass().add("time-step-selector");
        setSpacing(8);
        selector.setVisibleRowCount(14);
        selector.getItems().setAll(steps());
        selector.setValue(selector.getItems().stream()
                .filter(step -> "2d".equals(step.code()))
                .findFirst()
                .orElse(selector.getItems().get(0)));
        selector.valueProperty().addListener((observable, previous, value) -> {
            render(value);
            if (!silentSelection && !Objects.equals(previous, value)) {
                onChanged.run();
            }
        });
        HBox row = Ui.style(new HBox(10, Ui.fieldLabel("Pas", selector), selector, selected), "form-row", "time-step-selector-row");
        getChildren().addAll(row, detail);
        render(selector.getValue());
    }

    public String apiBucket() {
        DashboardTimeStep step = selector.getValue();
        return step == null ? "2d" : step.apiBucket();
    }

    public String displayCode() {
        DashboardTimeStep step = selector.getValue();
        return step == null ? "2d" : step.code();
    }

    public DashboardTimeStep selectedStep() {
        return selector.getValue();
    }

    public void selectCode(String code) {
        selectCode(code, false);
    }

    public void selectCodeSilently(String code) {
        selectCode(code, true);
    }

    public void onChanged(Runnable onChanged) {
        this.onChanged = onChanged == null ? () -> { } : onChanged;
    }

    public void setSelectorDisabled(boolean disabled) {
        selector.setDisable(disabled);
    }

    private void selectCode(String code, boolean silent) {
        if (code == null || code.isBlank()) {
            return;
        }
        DashboardTimeStep next = selector.getItems().stream()
                .filter(step -> code.equals(step.code()))
                .findFirst()
                .orElse(null);
        if (next == null || Objects.equals(selector.getValue(), next)) {
            return;
        }
        silentSelection = silent;
        try {
            selector.setValue(next);
        } finally {
            silentSelection = false;
        }
    }

    private void render(DashboardTimeStep step) {
        if (step == null) {
            selected.setText("—");
            detail.setText("Aucun pas sélectionné.");
            return;
        }
        selected.setText(step.code());
        detail.setText(step.help());
    }

    private List<DashboardTimeStep> steps() {
        return List.of(
                DashboardTimeStep.of("1s", "Seconde", "1s", "Un point par seconde. À utiliser sur une période courte pour éviter une série trop dense.", true),
                DashboardTimeStep.of("5s", "5 secondes", "5s", "Un point toutes les 5 secondes. À utiliser sur une période courte.", true),
                DashboardTimeStep.of("15s", "15 secondes", "15s", "Un point toutes les 15 secondes. À utiliser sur une période courte.", true),
                DashboardTimeStep.of("30s", "30 secondes", "30s", "Un point toutes les 30 secondes. À utiliser sur une période courte.", true),
                DashboardTimeStep.of("1min", "Minute", "1min", "Un point par minute.", true),
                DashboardTimeStep.of("5min", "5 minutes", "5min", "Un point toutes les 5 minutes.", true),
                DashboardTimeStep.of("15min", "15 minutes", "15min", "Un point toutes les 15 minutes.", true),
                DashboardTimeStep.of("30min", "30 minutes", "30min", "Un point toutes les 30 minutes.", true),
                DashboardTimeStep.of("1h", "Heure", "1h", "Un point par heure.", true),
                DashboardTimeStep.of("6h", "6 heures", "6h", "Un point toutes les 6 heures.", true),
                DashboardTimeStep.of("12h", "12 heures", "12h", "Un point toutes les 12 heures.", true),
                DashboardTimeStep.of("1d", "Jour", "1d", "Un point par jour.", false),
                DashboardTimeStep.of("2d", "2 jours", "2d", "Un point tous les deux jours, utile pour réduire le bruit visuel sur 30 jours.", false),
                DashboardTimeStep.of("3d", "3 jours", "3d", "Un point tous les trois jours.", false),
                DashboardTimeStep.of("1w", "Semaine", "1w", "Un point par semaine.", false),
                DashboardTimeStep.of("2w", "2 semaines", "2w", "Un point toutes les deux semaines.", false),
                DashboardTimeStep.of("1m", "Mois", "1m", "Un point par mois.", false),
                DashboardTimeStep.of("3m", "Trimestre", "3m", "Un point par trimestre.", false),
                DashboardTimeStep.of("6m", "Semestre", "6m", "Un point par semestre.", false),
                DashboardTimeStep.of("1y", "Année", "1y", "Un point par année.", false)
        );
    }
}
