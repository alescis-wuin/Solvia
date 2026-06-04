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
import fr.seynax.solvia.desktop.ui.EntriesView;
import fr.seynax.solvia.desktop.ui.SolviaShell;
import fr.seynax.solvia.desktop.ui.ThemeSupport;

public class SolviaDesktopApplication extends Application {

    private BackendStatusBanner backendStatusBanner;

    @Override
    public void start(Stage stage) {
        SolviaDesktopPreferences preferences = new SolviaDesktopPreferences();
        SolviaApiClient apiClient = new SolviaApiClient(preferences.backendUri());
        SolviaShell shell = new SolviaShell();

        AccountsView accountsView = new AccountsView(apiClient);
        EntriesView entriesView = new EntriesView(apiClient);
        AssetsPositionsView assetsPositionsView = new AssetsPositionsView(apiClient);
        DashboardView dashboardView = new DashboardView(
                apiClient,
                () -> shell.showPage("Saisie"),
                () -> shell.showPage("Comptes"),
                () -> shell.showPage("Actifs & positions")
        );
        assetsPositionsView.setOnPortfolioDataChanged(dashboardView::refresh);

        backendStatusBanner = new BackendStatusBanner(apiClient, preferences, status -> {
            propagateStatus(status, dashboardView, accountsView, entriesView, assetsPositionsView);
            shell.setStatus(status.message());
        });
        shell.setBackendStatusBanner(backendStatusBanner);

        shell.addPage("Dashboard", dashboardView);
        shell.addPage("Comptes", accountsView);
        shell.addPage("Actifs & positions", assetsPositionsView);
        shell.addPage("Saisie", entriesView);
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
            DashboardView dashboardView,
            AccountsView accountsView,
            EntriesView entriesView,
            AssetsPositionsView assetsPositionsView
    ) {
        dashboardView.backendStatusChanged(status);
        accountsView.backendStatusChanged(status);
        entriesView.backendStatusChanged(status);
        assetsPositionsView.backendStatusChanged(status);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
