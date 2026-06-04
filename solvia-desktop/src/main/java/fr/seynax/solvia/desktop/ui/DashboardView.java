package fr.seynax.solvia.desktop.ui;

import java.math.BigDecimal;
import java.time.LocalDate;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.layout.HBox;
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
    private final DashboardQuickLinksCard quickLinks = new DashboardQuickLinksCard();
    private final DashboardMetrics metrics = new DashboardMetrics();
    private final StateMessage state = new StateMessage();
    private final DashboardChartCard chart = new DashboardChartCard();
    private final DashboardQualityCard quality = new DashboardQualityCard();
    private final DashboardAllocationCard allocation = new DashboardAllocationCard();
    private final DashboardAccountsCard accounts = new DashboardAccountsCard();
    private volatile boolean backendReady;
    private boolean loadedOnce;

    public DashboardView(SolviaApiClient apiClient) {
        this(apiClient, null, null, null, null, null);
    }

    public DashboardView(SolviaApiClient apiClient, Runnable openDataEntry, Runnable openAccounts) {
        this(apiClient, null, openDataEntry, openAccounts, null, null);
    }

    public DashboardView(SolviaApiClient apiClient, Runnable openDataEntry, Runnable openAccounts, Runnable openAssets) {
        this(apiClient, null, openDataEntry, openAccounts, openAssets, null);
    }

    public DashboardView(SolviaApiClient apiClient, DesktopEventBus eventBus, Runnable openDataEntry, Runnable openAccounts, Runnable openAssets) {
        this(apiClient, eventBus, openDataEntry, openAccounts, openAssets, null);
    }

    public DashboardView(SolviaApiClient apiClient, DesktopEventBus eventBus, Runnable openDataEntry, Runnable openAccounts, Runnable openAssets, Runnable openSystem) {
        this.apiClient = apiClient;
        getStyleClass().add("content-view");
        setSpacing(18);
        setPadding(new Insets(20));
        filters.onRefresh(this::refresh);
        quickLinks.onRefresh(this::refresh);
        quickLinks.onDataEntry(openDataEntry);
        quickLinks.onAccounts(openAccounts);
        quickLinks.onAssets(openAssets);
        quickLinks.onSystem(openSystem);
        getChildren().addAll(topRow(), metrics, state, dashboardBody());
        VBox.setVgrow(chart, Priority.ALWAYS);
        showWaitingState("Vérification du backend local...");
    }

    public void backendStatusChanged(BackendStatusSnapshot snapshot) {
        boolean wasReady = backendReady;
        backendReady = snapshot.canLoadData();
        if (snapshot.state() == BackendConnectionState.CHECKING) {
            if (!loadedOnce) {
                showWaitingState("Vérification du backend local...");
            }
            return;
        }
        if (!backendReady) {
            loadedOnce = false;
            showUnavailableState(snapshot.message());
            return;
        }
        filters.setRefreshDisabled(false);
        quickLinks.setRefreshDisabled(false);
        if (!wasReady || !loadedOnce) {
            refresh();
        }
    }

    public void refresh() {
        if (!backendReady) {
            showUnavailableState("Backend local non prêt.");
            return;
        }
        LocalDate start = filters.from();
        LocalDate end = filters.to();
        if (start == null || end == null || start.isAfter(end)) {
            showErrorState("Période invalide. La date de début doit être avant ou égale à la date de fin.");
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

    private HBox topRow() {
        HBox row = Ui.style(new HBox(16, filters, quickLinks), "dashboard-top-row");
        HBox.setHgrow(filters, Priority.ALWAYS);
        return row;
    }

    private HBox dashboardBody() {
        VBox side = Ui.style(new VBox(18, quality, allocation, accounts), "dashboard-side");
        HBox body = Ui.style(new HBox(18, chart, side), "dashboard-body");
        HBox.setHgrow(chart, Priority.ALWAYS);
        return body;
    }

    private void update(DashboardPayload payload) {
        NetWorthDto netWorth = payload.data().netWorth();
        metrics.update(netWorth, payload.data().performance());
        chart.update(payload.series());
        quality.update(netWorth, payload.series(), filters);
        allocation.update(netWorth.allocation());
        accounts.update(netWorth.accounts());
        if (hasNoValuedData(netWorth)) {
            state.show("Aucune donnée patrimoniale", "Ajoute un compte puis une valeur de compte ou de position pour alimenter le dashboard.", "state-warning");
        } else if (payload.series().isEmpty()) {
            state.show("Aucune série", "Aucune valeur n'est disponible sur la période sélectionnée.", "state-warning");
        } else {
            state.hide();
        }
        loadedOnce = true;
        filters.setRefreshDisabled(false);
        quickLinks.setRefreshDisabled(false);
    }

    private boolean hasNoValuedData(NetWorthDto netWorth) {
        boolean noAccounts = netWorth.accounts() == null || netWorth.accounts().isEmpty();
        boolean noAllocation = netWorth.allocation() == null || netWorth.allocation().isEmpty();
        boolean totalIsZero = netWorth.total() == null
                || netWorth.total().amount() == null
                || BigDecimal.ZERO.compareTo(netWorth.total().amount()) == 0;
        return noAccounts && noAllocation && totalIsZero;
    }

    private void showWaitingState(String text) {
        filters.setRefreshDisabled(true);
        quickLinks.setRefreshDisabled(true);
        state.show("Vérification", text, "state-info");
    }

    private void showLoadingState() {
        filters.setRefreshDisabled(true);
        quickLinks.setRefreshDisabled(true);
        state.hide();
    }

    private void showUnavailableState(String text) {
        filters.setRefreshDisabled(true);
        quickLinks.setRefreshDisabled(true);
        metrics.clear();
        chart.clear();
        quality.clear();
        allocation.clear();
        accounts.clear();
        state.show("Backend non prêt", text, "state-warning");
    }

    private void showErrorState(String text) {
        filters.setRefreshDisabled(false);
        quickLinks.setRefreshDisabled(false);
        state.show("Erreur", text, "state-error");
    }

    private record DashboardData(NetWorthDto netWorth, PerformanceDto performance) {
    }

    private record DashboardPayload(DashboardData data, java.util.List<SeriesPointDto> series) {
    }
}
