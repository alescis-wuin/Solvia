package fr.seynax.solvia.desktop.ui;

import java.time.LocalDate;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import fr.seynax.solvia.desktop.api.ApiDtos.NetWorthDto;
import fr.seynax.solvia.desktop.api.ApiDtos.PerformanceDto;
import fr.seynax.solvia.desktop.api.ApiDtos.SeriesPointDto;
import fr.seynax.solvia.desktop.api.BackendConnectionState;
import fr.seynax.solvia.desktop.api.BackendStatusSnapshot;
import fr.seynax.solvia.desktop.api.SolviaApiClient;

public final class DashboardView extends VBox {

    private final SolviaApiClient apiClient;
    private final DashboardFilters filters = new DashboardFilters();
    private final DashboardMetrics metrics = new DashboardMetrics();
    private final StateMessage state = new StateMessage();
    private final DashboardChartCard chart = new DashboardChartCard();
    private final DashboardAllocationCard allocation = new DashboardAllocationCard();
    private volatile boolean backendReady;
    private boolean loadedOnce;

    public DashboardView(SolviaApiClient apiClient) {
        this.apiClient = apiClient;
        getStyleClass().add("content-view");
        setSpacing(18);
        setPadding(new Insets(20));
        filters.onRefresh(this::refresh);
        getChildren().addAll(filters, metrics, state, chart, allocation);
        VBox.setVgrow(chart, Priority.ALWAYS);
        showWaitingState("Verification du backend local...");
    }

    public void backendStatusChanged(BackendStatusSnapshot snapshot) {
        boolean wasReady = backendReady;
        backendReady = snapshot.canLoadData();
        if (snapshot.state() == BackendConnectionState.CHECKING) {
            if (!loadedOnce) {
                showWaitingState("Verification du backend local...");
            }
            return;
        }
        if (!backendReady) {
            loadedOnce = false;
            showUnavailableState(snapshot.message());
            return;
        }
        filters.setRefreshDisabled(false);
        if (!wasReady || !loadedOnce) {
            refresh();
        }
    }

    public void refresh() {
        if (!backendReady) {
            showUnavailableState("Backend local non pret.");
            return;
        }
        LocalDate start = filters.from();
        LocalDate end = filters.to();
        if (start == null || end == null || start.isAfter(end)) {
            showErrorState("Periode invalide. La date de debut doit etre avant ou egale a la date de fin.");
            return;
        }
        showLoadingState();
        apiClient.netWorth(end, "EUR")
                .thenCombine(apiClient.performance(start, end, "EUR"), DashboardData::new)
                .thenCombine(apiClient.netWorthSeries(start, end, filters.bucket(), filters.aggregation(), "EUR"), DashboardPayload::new)
                .whenComplete((payload, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        showErrorState(DesktopFormatters.errorMessage(error));
                        return;
                    }
                    update(payload);
                }));
    }

    private void update(DashboardPayload payload) {
        metrics.update(payload.data().netWorth(), payload.data().performance());
        chart.update(payload.series());
        allocation.update(payload.data().netWorth().allocation());
        if (payload.series().isEmpty()) {
            state.show("Aucune donnee", "Aucune valeur n'est disponible sur la periode.", "state-warning");
        } else {
            state.show("Donnees actualisees", "Le dashboard est a jour pour la periode selectionnee.", "state-success");
        }
        loadedOnce = true;
        filters.setRefreshDisabled(false);
    }

    private void showWaitingState(String text) {
        filters.setRefreshDisabled(true);
        state.show("Verification", text, "state-info");
    }

    private void showLoadingState() {
        filters.setRefreshDisabled(true);
        state.show("Chargement", "Chargement des donnees patrimoniales...", "state-info");
    }

    private void showUnavailableState(String text) {
        filters.setRefreshDisabled(true);
        metrics.clear();
        chart.clear();
        allocation.clear();
        state.show("Backend non pret", text, "state-warning");
    }

    private void showErrorState(String text) {
        filters.setRefreshDisabled(false);
        state.show("Erreur", text, "state-error");
    }

    private record DashboardData(NetWorthDto netWorth, PerformanceDto performance) {
    }

    private record DashboardPayload(DashboardData data, java.util.List<SeriesPointDto> series) {
    }
}
