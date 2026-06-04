package fr.seynax.solvia.desktop.ui;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;

public final class DashboardQuickLinksCard extends HBox {

    private final MenuButton menu = new MenuButton();
    private final MenuItem dataEntry = new MenuItem("Saisie");
    private final MenuItem assets = new MenuItem("Actifs & positions");
    private final MenuItem accounts = new MenuItem("Comptes");
    private final MenuItem refresh = new MenuItem("Actualiser");

    public DashboardQuickLinksCard() {
        getStyleClass().add("dashboard-toolbar");
        menu.getStyleClass().addAll("hamburger-menu", "icon-hamburger");
        menu.setText("☰");
        menu.setPopupSide(javafx.geometry.Side.BOTTOM);
        menu.getItems().addAll(dataEntry, assets, accounts, refresh);
        decorateMenuItems();
        getChildren().add(menu);
    }

    public void onDataEntry(Runnable task) {
        dataEntry.setOnAction(event -> execute(task));
    }

    public void onAssets(Runnable task) {
        assets.setOnAction(event -> execute(task));
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

    private void decorateMenuItems() {
        dataEntry.setGraphic(Ui.style(new javafx.scene.shape.SVGPath(), "menu-icon", "icon-entry"));
        assets.setGraphic(Ui.style(new javafx.scene.shape.SVGPath(), "menu-icon", "icon-assets"));
        accounts.setGraphic(Ui.style(new javafx.scene.shape.SVGPath(), "menu-icon", "icon-accounts"));
        refresh.setGraphic(Ui.style(new javafx.scene.shape.SVGPath(), "menu-icon", "icon-refresh"));
    }

    private void execute(Runnable task) {
        if (task != null) {
            task.run();
        }
    }
}
