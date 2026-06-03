package fr.seynax.solvia.desktop;

import java.net.URI;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import fr.seynax.solvia.desktop.api.SolviaApiClient;
import fr.seynax.solvia.desktop.ui.AccountsView;
import fr.seynax.solvia.desktop.ui.DashboardView;
import fr.seynax.solvia.desktop.ui.EntriesView;
import fr.seynax.solvia.desktop.ui.SolviaShell;
import fr.seynax.solvia.desktop.ui.ThemeSupport;

public class SolviaDesktopApplication extends Application {

    @Override
    public void start(Stage stage) {
        SolviaApiClient apiClient = new SolviaApiClient(URI.create("http://127.0.0.1:8080"));
        SolviaShell shell = new SolviaShell();

        AccountsView accountsView = new AccountsView(apiClient);
        EntriesView entriesView = new EntriesView(apiClient);
        DashboardView dashboardView = new DashboardView(apiClient);

        shell.addPage("Dashboard", dashboardView);
        shell.addPage("Accounts", accountsView);
        shell.addPage("Data entry", entriesView);
        shell.setStatus("Start the backend with: mvn -pl solvia-backend -am spring-boot:run");

        Scene scene = new Scene(shell, 1220, 800);
        ThemeSupport.install(scene);

        stage.setTitle("Solvia");
        stage.setMinWidth(1100);
        stage.setMinHeight(720);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
