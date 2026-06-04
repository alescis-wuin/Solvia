package fr.seynax.solvia.desktop.ui;

import java.util.List;

import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class DashboardTimeStepSelector extends VBox {

    private final ComboBox<DashboardTimeStep> selector = Ui.tooltip(new ComboBox<>(), "Sélectionne le pas de lecture du graphique.");
    private final Label selected = Ui.label("—", "monospace");
    private final Label detail = Ui.help("—");

    public DashboardTimeStepSelector() {
        getStyleClass().add("time-step-selector");
        setSpacing(8);
        selector.setVisibleRowCount(12);
        selector.getItems().setAll(steps());
        selector.setValue(selector.getItems().stream()
                .filter(step -> "2d".equals(step.code()))
                .findFirst()
                .orElse(selector.getItems().get(0)));
        selector.valueProperty().addListener((observable, previous, value) -> render(value));
        HBox row = Ui.style(new HBox(10, Ui.fieldLabel("Pas", selector), selector, selected), "form-row");
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

    public void setSelectorDisabled(boolean disabled) {
        selector.setDisable(disabled);
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
                DashboardTimeStep.of("1s", "Seconde", "1d", "Précision demandée à la seconde. Les données V1 sont datées au jour : affichage ramené au pas journalier.", true),
                DashboardTimeStep.of("5s", "5 secondes", "1d", "Préréglage fin disponible côté UI. La série V1 reste agrégée par jour.", true),
                DashboardTimeStep.of("15s", "15 secondes", "1d", "Préréglage fin disponible côté UI. La série V1 reste agrégée par jour.", true),
                DashboardTimeStep.of("30s", "30 secondes", "1d", "Préréglage fin disponible côté UI. La série V1 reste agrégée par jour.", true),
                DashboardTimeStep.of("1min", "Minute", "1d", "Précision à la minute prévue pour une future granularité temporelle. En V1, les snapshots sont journaliers.", true),
                DashboardTimeStep.of("5min", "5 minutes", "1d", "Préréglage fin prévu pour une future granularité temporelle. En V1, les snapshots sont journaliers.", true),
                DashboardTimeStep.of("15min", "15 minutes", "1d", "Préréglage fin prévu pour une future granularité temporelle. En V1, les snapshots sont journaliers.", true),
                DashboardTimeStep.of("30min", "30 minutes", "1d", "Préréglage fin prévu pour une future granularité temporelle. En V1, les snapshots sont journaliers.", true),
                DashboardTimeStep.of("1h", "Heure", "1d", "Précision horaire prévue côté UI. Le backend V1 calcule encore par date.", true),
                DashboardTimeStep.of("6h", "6 heures", "1d", "Préréglage horaire prévu côté UI. Le backend V1 calcule encore par date.", true),
                DashboardTimeStep.of("12h", "12 heures", "1d", "Préréglage horaire prévu côté UI. Le backend V1 calcule encore par date.", true),
                DashboardTimeStep.of("1d", "Jour", "1d", "Un point par jour. C'est la granularité native actuelle des snapshots Solvia.", false),
                DashboardTimeStep.of("2d", "2 jours", "2d", "Un point tous les deux jours, utile pour réduire le bruit visuel sur 30 jours.", false),
                DashboardTimeStep.of("3d", "3 jours", "3d", "Un point tous les trois jours.", false),
                DashboardTimeStep.of("1w", "Semaine", "1w", "Un point par semaine.", false),
                DashboardTimeStep.of("2w", "2 semaines", "2w", "Un point toutes les deux semaines.", false),
                DashboardTimeStep.of("1m", "Mois", "1m", "Un point par mois.", false),
                DashboardTimeStep.of("3m", "Trimestre", "3m", "Un point par trimestre.", false),
                DashboardTimeStep.of("6m", "Semestre", "6m", "Un point par semestre.", false),
                DashboardTimeStep.of("1y", "Année", "12m", "Un point par année, converti en 12 mois pour le backend V1.", false)
        );
    }
}
