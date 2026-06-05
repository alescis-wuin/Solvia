package fr.seynax.solvia.desktop.ui;

import java.net.URI;
import java.util.Objects;
import java.util.function.Consumer;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import fr.seynax.solvia.desktop.SolviaDesktopPreferences;
import fr.seynax.solvia.desktop.api.BackendConnectionState;
import fr.seynax.solvia.desktop.api.BackendStatusSnapshot;
import fr.seynax.solvia.desktop.api.SolviaApiClient;
import fr.seynax.solvia.desktop.backend.BackendLauncher;

public final class BackendStatusBanner extends VBox {

    private static final Duration CHECK_INTERVAL = Duration.seconds(20);

    private final SolviaApiClient apiClient;
    private final SolviaDesktopPreferences preferences;
    private final BackendLauncher backendLauncher;
    private final Consumer<BackendStatusSnapshot> statusListener;
    private final Label state = new Label("Vérification...");
    private final Label message = new Label("Vérification du backend local...");
    private final Label details = new Label("—");
    private final TextField backendUrl = new TextField();
    private final Button apply = new Button("Appliquer");
    private final Button retry = new Button("Réessayer");
    private final Button startBackend = new Button("Démarrer backend");
    private final Timeline timeline;
    private volatile boolean checking;
    private BackendStatusSnapshot lastPublishedSnapshot;

    public BackendStatusBanner(
            SolviaApiClient apiClient,
            SolviaDesktopPreferences preferences,
            Consumer<BackendStatusSnapshot> statusListener
    ) {
        this.apiClient = Objects.requireNonNull(apiClient, "api client is required");
        this.preferences = Objects.requireNonNull(preferences, "desktop preferences are required");
        this.statusListener = Objects.requireNonNull(statusListener, "status listener is required");
        this.backendLauncher = new BackendLauncher();
        this.backendUrl.setText(apiClient.baseUri().toString());
        this.timeline = new Timeline(new KeyFrame(CHECK_INTERVAL, event -> checkNow()));
        this.timeline.setCycleCount(Timeline.INDEFINITE);
        build();
    }

    public void start() {
        checkNow();
        timeline.playFromStart();
    }

    public void stop() {
        timeline.stop();
        backendLauncher.stop();
    }

    public void checkNow() {
        if (checking) {
            return;
        }
        checking = true;
        update(BackendStatusSnapshot.checking(apiClient.baseUri().toString()));
        apiClient.readiness().whenComplete((snapshot, error) -> Platform.runLater(() -> {
            checking = false;
            if (error != null) {
                update(BackendStatusSnapshot.unavailable(apiClient.baseUri().toString(), DesktopFormatters.errorMessage(error)));
                return;
            }
            update(snapshot);
        }));
    }

    private void build() {
        getStyleClass().add("status-banner");
        state.getStyleClass().add("status-state");
        message.getStyleClass().add("status-message");
        details.getStyleClass().add("help-text");
        details.setWrapText(true);
        backendUrl.getStyleClass().add("backend-url-field");
        backendUrl.setTooltip(new Tooltip("URL du backend local utilisé par l’application desktop."));
        HBox.setHgrow(backendUrl, Priority.ALWAYS);

        apply.setTooltip(new Tooltip("Enregistrer l’URL et relancer la vérification."));
        retry.setTooltip(new Tooltip("Relancer immédiatement la vérification du backend."));
        startBackend.setTooltip(new Tooltip("Tente de lancer le backend avec Maven depuis la racine du projet. Action explicite uniquement."));
        apply.setOnAction(event -> applyBackendUrl());
        retry.setOnAction(event -> checkNow());
        startBackend.setOnAction(event -> startBackend());

        HBox top = new HBox(12, state, message);
        top.setAlignment(Pos.CENTER_LEFT);
        HBox controls = new HBox(10, new Label("Backend"), backendUrl, apply, retry, startBackend);
        controls.setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(top, details, controls);
    }

    private void applyBackendUrl() {
        try {
            URI uri = preferences.saveBackendUri(backendUrl.getText());
            apiClient.updateBaseUri(uri);
            backendUrl.setText(uri.toString());
            checkNow();
        } catch (IllegalArgumentException exception) {
            update(BackendStatusSnapshot.error(
                    backendUrl.getText(),
                    exception.getMessage(),
                    exception.getMessage()
            ));
        }
    }

    private void startBackend() {
        startBackend.setDisable(true);
        message.setText("Démarrage du backend local...");
        details.setText("Commande: mvn -pl solvia-backend -am spring-boot:run");
        backendLauncher.start().whenComplete((result, error) -> Platform.runLater(() -> {
            startBackend.setDisable(false);
            if (error != null) {
                update(BackendStatusSnapshot.error(apiClient.baseUri().toString(), "Démarrage backend impossible.", DesktopFormatters.errorMessage(error)));
                return;
            }
            if (!result.success()) {
                update(BackendStatusSnapshot.error(apiClient.baseUri().toString(), result.message(), result.message()));
                return;
            }
            message.setText(result.message());
            checkNow();
        }));
    }

    private void update(BackendStatusSnapshot snapshot) {
        render(snapshot);
        if (shouldPublish(snapshot)) {
            lastPublishedSnapshot = snapshot;
            statusListener.accept(snapshot);
        }
    }

    private void render(BackendStatusSnapshot snapshot) {
        getStyleClass().removeAll(
                "status-checking",
                "status-connected",
                "status-degraded",
                "status-unavailable",
                "status-error"
        );
        getStyleClass().add(styleClass(snapshot.state()));
        state.setText(label(snapshot.state()));
        message.setText(snapshot.message());
        details.setText(details(snapshot));
        retry.setDisable(snapshot.state() == BackendConnectionState.CHECKING);
        startBackend.setDisable(snapshot.state() == BackendConnectionState.CHECKING || backendLauncher.running());
    }

    private boolean shouldPublish(BackendStatusSnapshot snapshot) {
        if (lastPublishedSnapshot == null) {
            return true;
        }
        if (snapshot.state() == BackendConnectionState.CHECKING) {
            return !lastPublishedSnapshot.canLoadData()
                    && lastPublishedSnapshot.state() != BackendConnectionState.CHECKING;
        }
        return snapshot.state() != lastPublishedSnapshot.state()
                || snapshot.canLoadData() != lastPublishedSnapshot.canLoadData()
                || !Objects.equals(snapshot.baseUrl(), lastPublishedSnapshot.baseUrl())
                || !Objects.equals(snapshot.databaseStatus(), lastPublishedSnapshot.databaseStatus())
                || !Objects.equals(snapshot.message(), lastPublishedSnapshot.message())
                || !Objects.equals(snapshot.technicalMessage(), lastPublishedSnapshot.technicalMessage());
    }

    private String details(BackendStatusSnapshot snapshot) {
        String checkedAt = DesktopFormatters.time(snapshot.checkedAt());
        String database = snapshot.databaseStatus() == null ? "UNKNOWN" : snapshot.databaseStatus();
        String technical = snapshot.technicalMessage() == null ? "" : " — " + snapshot.technicalMessage();
        return "URL: " + snapshot.baseUrl() + " — PostgreSQL: " + database + " — Dernier contrôle: " + checkedAt + technical;
    }

    private String label(BackendConnectionState state) {
        return switch (state) {
            case CHECKING -> "Vérification";
            case CONNECTED -> "Connecté";
            case DEGRADED -> "Dégradé";
            case UNAVAILABLE -> "Indisponible";
            case ERROR -> "Erreur";
        };
    }

    private String styleClass(BackendConnectionState state) {
        return switch (state) {
            case CHECKING -> "status-checking";
            case CONNECTED -> "status-connected";
            case DEGRADED -> "status-degraded";
            case UNAVAILABLE -> "status-unavailable";
            case ERROR -> "status-error";
        };
    }
}
