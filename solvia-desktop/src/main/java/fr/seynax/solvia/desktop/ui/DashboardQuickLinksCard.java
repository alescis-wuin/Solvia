package fr.seynax.solvia.desktop.ui;

import javafx.scene.control.Button;
import javafx.scene.layout.HBox;

public final class DashboardQuickLinksCard extends SectionCard {

    private final Button dataEntry = new Button("Ajouter une saisie");
    private final Button accounts = new Button("Gérer les comptes");
    private final Button refresh = new Button("Actualiser");

    public DashboardQuickLinksCard() {
        super("Raccourcis", "Accès direct aux workflows quotidiens du suivi patrimonial.");
        dataEntry.setTooltip(new javafx.scene.control.Tooltip("Ouvre l'écran de saisie des valeurs et des flux."));
        accounts.setTooltip(new javafx.scene.control.Tooltip("Ouvre l'écran des comptes."));
        refresh.setTooltip(new javafx.scene.control.Tooltip("Recharge le dashboard."));
        dataEntry.getStyleClass().add("primary-action");
        HBox row = Ui.style(new HBox(12, dataEntry, accounts, refresh), "dashboard-action-row");
        getChildren().add(row);
    }

    public void onDataEntry(Runnable task) {
        dataEntry.setOnAction(event -> execute(task));
    }

    public void onAccounts(Runnable task) {
        accounts.setOnAction(event -> execute(task));
    }

    public void onRefresh(Runnable task) {
        refresh.setOnAction(event -> execute(task));
    }

    public void setRefreshDisabled(boolean disabled) {
        refresh.setDisable(disabled);
    }

    private void execute(Runnable task) {
        if (task != null) {
            task.run();
        }
    }
}
