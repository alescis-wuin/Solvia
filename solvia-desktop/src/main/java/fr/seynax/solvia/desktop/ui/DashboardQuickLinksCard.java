package fr.seynax.solvia.desktop.ui;

import javafx.geometry.Side;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.shape.SVGPath;

public final class DashboardQuickLinksCard extends HBox {

    private static final String HAMBURGER_ICON = "M3 6h18v2H3z M3 11h18v2H3z M3 16h18v2H3z";
    private static final String ENTRY_ICON = "M4 4h16v3H4z M4 10h16v3H4z M4 16h10v3H4z";
    private static final String ASSETS_ICON = "M12 2l9 5v10l-9 5-9-5V7z M12 5L6 8v8l6 3 6-3V8z";
    private static final String ACCOUNTS_ICON = "M4 5h16v4H4z M4 11h16v8H4z M7 14h4v2H7z";
    private static final String SYSTEM_ICON = "M12 8a4 4 0 100 8 4 4 0 000-8z M12 2l2 3 4 .5-.8 3.7 2.8 2.8-2.8 2.8.8 3.7-4 .5-2 3-2-3-4-.5.8-3.7L3 12l2.8-2.8L5 5.5 9 5z";
    private static final String REFRESH_ICON = "M17 6a7 7 0 10-1 10l-2-2h7V7l-2 2A5 5 0 105 12H3a7 7 0 1112 5z";

    private final Button menu = new Button();
    private final ContextMenu contextMenu = new ContextMenu();
    private final MenuItem dataEntry = new MenuItem("Saisie");
    private final MenuItem assets = new MenuItem("Actifs & positions");
    private final MenuItem accounts = new MenuItem("Comptes");
    private final MenuItem system = new MenuItem("Système");
    private final MenuItem refresh = new MenuItem("Actualiser");

    public DashboardQuickLinksCard() {
        getStyleClass().add("dashboard-toolbar");
        menu.getStyleClass().addAll("hamburger-menu", "icon-button");
        menu.setGraphic(icon(HAMBURGER_ICON, "icon-hamburger"));
        menu.setTooltip(new javafx.scene.control.Tooltip("Ouvrir le menu du dashboard."));
        menu.setOnAction(event -> contextMenu.show(menu, Side.BOTTOM, 0, 6));
        contextMenu.getItems().addAll(dataEntry, assets, accounts, system, refresh);
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
        dataEntry.setGraphic(icon(ENTRY_ICON, "icon-entry"));
        assets.setGraphic(icon(ASSETS_ICON, "icon-assets"));
        accounts.setGraphic(icon(ACCOUNTS_ICON, "icon-accounts"));
        system.setGraphic(icon(SYSTEM_ICON, "icon-system"));
        refresh.setGraphic(icon(REFRESH_ICON, "icon-refresh"));
    }

    private SVGPath icon(String content, String styleClass) {
        SVGPath icon = new SVGPath();
        icon.setContent(content);
        return Ui.style(icon, "menu-icon", styleClass);
    }

    private void execute(Runnable task) {
        contextMenu.hide();
        if (task != null) {
            task.run();
        }
    }
}
