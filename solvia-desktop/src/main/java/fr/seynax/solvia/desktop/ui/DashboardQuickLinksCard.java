package fr.seynax.solvia.desktop.ui;

import javafx.geometry.Side;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.shape.SVGPath;

public final class DashboardQuickLinksCard extends HBox {

    private final MenuButton menu = new MenuButton();
    private final MenuItem dataEntry = new MenuItem("Saisie");
    private final MenuItem assets = new MenuItem("Actifs & positions");
    private final MenuItem accounts = new MenuItem("Comptes");
    private final MenuItem system = new MenuItem("Système");
    private final MenuItem refresh = new MenuItem("Actualiser");

    public DashboardQuickLinksCard() {
        getStyleClass().add("dashboard-toolbar");
        menu.getStyleClass().addAll("hamburger-menu", "icon-hamburger");
        menu.setText("☰");
        menu.setPopupSide(Side.BOTTOM);
        menu.getItems().addAll(dataEntry, assets, accounts, system, refresh);
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

    public void onSystem(Runnable task) {
        system.setOnAction(event -> execute(task));
    }

    public void onRefresh(Runnable task) {
        refresh.setOnAction(event -> execute(task));
    }

    public void setRefreshDisabled(boolean disabled) {
        refresh.setDisable(disabled);
    }

    private void decorateMenuItems() {
        dataEntry.setGraphic(icon("icon-entry"));
        assets.setGraphic(icon("icon-assets"));
        accounts.setGraphic(icon("icon-accounts"));
        system.setGraphic(icon("icon-system"));
        refresh.setGraphic(icon("icon-refresh"));
    }

    private SVGPath icon(String styleClass) {
        return Ui.style(new SVGPath(), "menu-icon", styleClass);
    }

    private void execute(Runnable task) {
        if (task != null) {
            task.run();
        }
    }
}
