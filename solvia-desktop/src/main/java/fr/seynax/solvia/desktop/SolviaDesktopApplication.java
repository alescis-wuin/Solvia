package fr.seynax.solvia.desktop;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import fr.seynax.solvia.desktop.api.BackendStatusSnapshot;
import fr.seynax.solvia.desktop.api.SolviaApiClient;
import fr.seynax.solvia.desktop.ui.AccountsView;
import fr.seynax.solvia.desktop.ui.AssetsPositionsView;
import fr.seynax.solvia.desktop.ui.BackendStatusBanner;
import fr.seynax.solvia.desktop.ui.DashboardView;
import fr.seynax.solvia.desktop.ui.DesktopEventBus;
import fr.seynax.solvia.desktop.ui.DesktopEventBus.EventType;
import fr.seynax.solvia.desktop.ui.EntriesView;
import fr.seynax.solvia.desktop.ui.SolviaShell;
import fr.seynax.solvia.desktop.ui.SystemView;
import fr.seynax.solvia.desktop.ui.ThemeSupport;

public class SolviaDesktopApplication extends Application {

    private BackendStatusBanner backendStatusBanner;

    @Override
    public void start(Stage stage) {
        SolviaDesktopPreferences preferences = new SolviaDesktopPreferences();
        SolviaApiClient apiClient = new SolviaApiClient(preferences.backendUri());
        DesktopEventBus eventBus = new DesktopEventBus();
        SolviaShell shell = new SolviaShell();

        AccountsView accountsView = new AccountsView(apiClient);
        EntriesView entriesView = new EntriesView(apiClient);
        AssetsPositionsView assetsPositionsView = new AssetsPositionsView(apiClient);
        backendStatusBanner = new BackendStatusBanner(apiClient, preferences, status -> {
            propagateStatus(status, shell, null, accountsView, entriesView, assetsPositionsView);
        });
        SystemView systemView = new SystemView(backendStatusBanner);
        DashboardView dashboardView = new DashboardView(
                apiClient,
                eventBus,
                () -> shell.showPage("Saisie"),
                () -> shell.showPage("Comptes"),
                () -> shell.showPage("Actifs & positions"),
                () -> shell.showPage("Système")
        );

        eventBus.subscribe(EventType.ACCOUNTS_CHANGED, () -> {
            entriesView.refreshAccounts();
            assetsPositionsView.refresh();
        });
        eventBus.subscribe(EventType.PORTFOLIO_DATA_CHANGED, dashboardView::refresh);
        accountsView.setOnAccountsChanged(() -> {
            eventBus.publish(EventType.ACCOUNTS_CHANGED);
            eventBus.publish(EventType.PORTFOLIO_DATA_CHANGED);
        });
        entriesView.setOnPortfolioDataChanged(() -> eventBus.publish(EventType.PORTFOLIO_DATA_CHANGED));
        assetsPositionsView.setOnPortfolioDataChanged(() -> eventBus.publish(EventType.PORTFOLIO_DATA_CHANGED));

        shell.addPage("Dashboard", dashboardView);
        shell.addPage("Comptes", accountsView);
        shell.addPage("Actifs & positions", assetsPositionsView);
        shell.addPage("Saisie", entriesView);
        shell.addPage("Système", systemView);
        shell.setStatus("Start the backend with: mvn -pl solvia-backend -am spring-boot:run");

        Scene scene = new Scene(shell, 1220, 800);
        ThemeSupport.install(scene);

        stage.setTitle("Solvia");
        stage.setMinWidth(1100);
        stage.setMinHeight(720);
        stage.setScene(scene);
        stage.setOnShown(event -> backendStatusBanner.start());
        stage.setOnCloseRequest(event -> backendStatusBanner.stop());
        stage.show();
    }

    private void propagateStatus(
            BackendStatusSnapshot status,
            SolviaShell shell,
            DashboardView dashboardView,
            AccountsView accountsView,
            EntriesView entriesView,
            AssetsPositionsView assetsPositionsView
    ) {
        if (dashboardView != null) {
            dashboardView.backendStatusChanged(status);
        }
        accountsView.backendStatusChanged(status);
        entriesView.backendStatusChanged(status);
        assetsPositionsView.backendStatusChanged(status);
        shell.setStatus(status.message());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
