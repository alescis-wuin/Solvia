package fr.seynax.solvia.desktop.ui;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import fr.seynax.solvia.desktop.api.ApiDtos.NetWorthDto;
import fr.seynax.solvia.desktop.api.ApiDtos.PerformanceDto;
import fr.seynax.solvia.desktop.api.ApiDtos.SeriesPointDto;
import fr.seynax.solvia.desktop.api.BackendConnectionState;
import fr.seynax.solvia.desktop.api.BackendStatusSnapshot;
import fr.seynax.solvia.desktop.api.SolviaApiClient;

public final class DashboardView extends VBox {

    private static final DateTimeFormatter HEADER_DATE = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRANCE);

    private final SolviaApiClient apiClient;
    private final DashboardFilters filters = new DashboardFilters();
    private final DashboardQuickLinksCard quickLinks = new DashboardQuickLinksCard();
    private final DashboardMetrics metrics = new DashboardMetrics();
    private final StateMessage state = new StateMessage();
    private final DashboardChartCard chart = new DashboardChartCard();
    private final DashboardQualityCard quality = new DashboardQualityCard();
    private final DashboardAllocationCard allocation = new DashboardAllocationCard();
    private final DashboardAccountsCard accounts = new DashboardAccountsCard();
    private final AtomicLong refreshGeneration = new AtomicLong();
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
        getStyleClass().addAll("content-view", "dashboard-view");
        setSpacing(22);
        setPadding(new Insets(30, 34, 44, 34));
        filters.onRefresh(this::refresh);
        quickLinks.onRefresh(this::refresh);
        quickLinks.onDataEntry(openDataEntry);
        quickLinks.onAccounts(openAccounts);
        quickLinks.onAssets(openAssets);
        quickLinks.onSystem(openSystem);
        getChildren().addAll(dashboardHeader(openDataEntry), metrics, state, topRow(), dashboardBody());
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
            refreshGeneration.incrementAndGet();
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
        long generation = refreshGeneration.incrementAndGet();
        FocusSnapshot focus = FocusSnapshot.capture(this);
        showLoadingState();
        apiClient.netWorth(end, "EUR")
                .thenCombine(apiClient.performance(start, end, "EUR"), DashboardData::new)
                .thenCombine(apiClient.netWorthSeries(start, end, filters.bucket(), filters.aggregation(), "EUR"), DashboardPayload::new)
                .whenComplete((payload, error) -> Platform.runLater(() -> {
                    if (generation != refreshGeneration.get()) {
                        focus.restoreLater();
                        return;
                    }
                    if (error != null) {
                        showErrorState(DesktopFormatters.errorMessage(error));
                        focus.restoreLater();
                        return;
                    }
                    update(payload);
                    focus.restoreLater();
                }));
    }

    private HBox dashboardHeader(Runnable openDataEntry) {
        Label title = Ui.label("Bonjour", "dashboard-hello");
        Label date = Ui.label(LocalDate.now().format(HEADER_DATE) + " · Données locales", "dashboard-subtitle");
        VBox copy = new VBox(4, title, date);

        TextField search = Ui.tooltip(new TextField(), "Recherche visuelle. La recherche active sera branchée quand les flux d'actifs exposeront une liste valorisée.");
        search.setPromptText("Rechercher un actif...");
        search.getStyleClass().add("dashboard-search");
        search.setDisable(true);

        Button invest = Ui.tooltip(new Button("+ Investir"), "Ouvre la saisie pour ajouter une valorisation ou un flux.");
        invest.getStyleClass().addAll("dashboard-invest-button", "primary-action");
        invest.setOnAction(event -> {
            if (openDataEntry != null) {
                openDataEntry.run();
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(16, copy, spacer, search, invest, quickLinks);
        row.getStyleClass().add("dashboard-header");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox topRow() {
        HBox row = Ui.style(new HBox(16, filters), "dashboard-top-row");
        HBox.setHgrow(filters, Priority.ALWAYS);
        return row;
    }

    private HBox dashboardBody() {
        VBox main = Ui.style(new VBox(18, chart, accounts), "dashboard-main-column");
        VBox side = Ui.style(new VBox(18, allocation, quality), "dashboard-side");
        HBox body = Ui.style(new HBox(18, main, side), "dashboard-body");
        HBox.setHgrow(main, Priority.ALWAYS);
        VBox.setVgrow(chart, Priority.ALWAYS);
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
        filters.setRefreshDisabled(false);
        quickLinks.setRefreshDisabled(false);
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
