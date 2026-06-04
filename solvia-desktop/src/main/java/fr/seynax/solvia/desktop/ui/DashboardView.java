package fr.seynax.solvia.desktop.ui;

import java.math.BigDecimal;
import java.time.LocalDate;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import fr.seynax.solvia.desktop.api.ApiDtos.AssetTypeValueDto;
import fr.seynax.solvia.desktop.api.ApiDtos.NetWorthDto;
import fr.seynax.solvia.desktop.api.ApiDtos.PerformanceDto;
import fr.seynax.solvia.desktop.api.ApiDtos.SeriesPointDto;
import fr.seynax.solvia.desktop.api.BackendConnectionState;
import fr.seynax.solvia.desktop.api.BackendStatusSnapshot;
import fr.seynax.solvia.desktop.api.SolviaApiClient;

public final class DashboardView extends VBox {

    private final SolviaApiClient apiClient;
    private final DatePicker from = new DatePicker(LocalDate.now().minusDays(30));
    private final DatePicker to = new DatePicker(LocalDate.now());
    private final ComboBox<String> bucket = new ComboBox<>();
    private final ComboBox<String> aggregation = new ComboBox<>();
    private final Label total = new Label("—");
    private final Label grossChange = new Label("—");
    private final Label adjustedGain = new Label("—");
    private final Label status = new Label("Vérification du backend local...");
    private final Button refresh = new Button("Actualiser");
    private final LineChart<Number, Number> chart;
    private final TableView<AllocationRow> allocation = new TableView<>();
    private volatile boolean backendReady;

    public DashboardView(SolviaApiClient apiClient) {
        this.apiClient = apiClient;
        this.chart = createChart();
        getStyleClass().add("content-view");
        setSpacing(16);
        setPadding(new Insets(20));
        bucket.getItems().setAll("1d", "2d", "1w", "1m");
        bucket.setValue("2d");
        aggregation.getItems().setAll("last", "average");
        aggregation.setValue("last");
        status.getStyleClass().add("help-text");
        total.getStyleClass().add("metric-value");
        grossChange.getStyleClass().add("metric-value");
        adjustedGain.getStyleClass().add("metric-value");
        getChildren().addAll(filters(), metrics(), status, chart, allocationTable());
        VBox.setVgrow(chart, Priority.ALWAYS);
        showWaitingState("Vérification du backend local...");
    }

    public void backendStatusChanged(BackendStatusSnapshot snapshot) {
        backendReady = snapshot.canLoadData();
        if (snapshot.state() == BackendConnectionState.CHECKING) {
            showWaitingState("Vérification du backend local...");
            return;
        }
        if (!backendReady) {
            showUnavailableState(snapshot.message());
            return;
        }
        refresh();
    }

    private HBox filters() {
        refresh.setOnAction(event -> refresh());
        refresh.setDisable(true);
        HBox filters = new HBox(12, new Label("Du"), from, new Label("au"), to,
                new Label("Pas"), bucket, new Label("Agrégation"), aggregation, refresh);
        filters.getStyleClass().add("form-card");
        return filters;
    }

    private GridPane metrics() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("card");
        grid.setHgap(16);
        grid.setVgap(6);
        grid.add(new Label("Patrimoine"), 0, 0);
        grid.add(total, 0, 1);
        grid.add(new Label("Variation brute"), 1, 0);
        grid.add(grossChange, 1, 1);
        grid.add(new Label("Gain corrigé des flux"), 2, 0);
        grid.add(adjustedGain, 2, 1);
        return grid;
    }

    private LineChart<Number, Number> createChart() {
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Jours");
        yAxis.setLabel("Patrimoine");
        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Historique du patrimoine");
        lineChart.setLegendVisible(false);
        lineChart.getStyleClass().add("card");
        return lineChart;
    }

    private TableView<AllocationRow> allocationTable() {
        TableColumn<AllocationRow, String> type = new TableColumn<>("Classe d’actifs");
        type.setCellValueFactory(new PropertyValueFactory<>("assetType"));
        TableColumn<AllocationRow, String> value = new TableColumn<>("Valeur");
        value.setCellValueFactory(new PropertyValueFactory<>("value"));
        TableColumn<AllocationRow, String> share = new TableColumn<>("Part");
        share.setCellValueFactory(new PropertyValueFactory<>("share"));
        allocation.getColumns().setAll(type, value, share);
        allocation.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        allocation.setPrefHeight(180);
        return allocation;
    }

    public void refresh() {
        if (!backendReady) {
            showUnavailableState("Backend local non prêt.");
            return;
        }
        LocalDate start = from.getValue();
        LocalDate end = to.getValue();
        showLoadingState();
        apiClient.netWorth(end, "EUR")
                .thenCombine(apiClient.performance(start, end, "EUR"), DashboardData::new)
                .thenCombine(apiClient.netWorthSeries(start, end, bucket.getValue(), aggregation.getValue(), "EUR"), DashboardPayload::new)
                .whenComplete((payload, error) -> Platform.runLater(() -> {
                    if (error != null) {
                        showErrorState(DesktopFormatters.errorMessage(error));
                        return;
                    }
                    update(payload);
                }));
    }

    private void update(DashboardPayload payload) {
        NetWorthDto netWorth = payload.data().netWorth();
        PerformanceDto performance = payload.data().performance();
        total.setText(DesktopFormatters.money(netWorth.total()));
        grossChange.setText(DesktopFormatters.signedMoney(performance.grossChange()) + " / "
                + DesktopFormatters.percent(performance.grossChangePercentage()));
        adjustedGain.setText(DesktopFormatters.signedMoney(performance.flowAdjustedGain()) + " / "
                + DesktopFormatters.percent(performance.flowAdjustedGainPercentage()));
        chart.getData().setAll(series(payload.series()));
        allocation.getItems().setAll(netWorth.allocation().stream().map(AllocationRow::from).toList());
        status.setText(payload.series().isEmpty() ? "Aucune donnée sur la période." : "Données actualisées.");
        refresh.setDisable(false);
    }

    private void showWaitingState(String text) {
        refresh.setDisable(true);
        status.setText(text);
    }

    private void showLoadingState() {
        refresh.setDisable(true);
        status.setText("Chargement des données patrimoniales...");
    }

    private void showUnavailableState(String text) {
        refresh.setDisable(true);
        total.setText("—");
        grossChange.setText("—");
        adjustedGain.setText("—");
        chart.getData().clear();
        allocation.getItems().clear();
        status.setText(text);
    }

    private void showErrorState(String text) {
        refresh.setDisable(false);
        status.setText(text);
    }

    private XYChart.Series<Number, Number> series(java.util.List<SeriesPointDto> points) {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        LocalDate start = from.getValue();
        for (SeriesPointDto point : points) {
            long day = java.time.temporal.ChronoUnit.DAYS.between(start, point.valueDate());
            BigDecimal amount = point.value().amount();
            series.getData().add(new XYChart.Data<>(day, amount.doubleValue()));
        }
        return series;
    }

    private record DashboardData(NetWorthDto netWorth, PerformanceDto performance) {
    }

    private record DashboardPayload(DashboardData data, java.util.List<SeriesPointDto> series) {
    }

    public static final class AllocationRow {
        private final String assetType;
        private final String value;
        private final String share;

        private AllocationRow(String assetType, String value, String share) {
            this.assetType = assetType;
            this.value = value;
            this.share = share;
        }

        static AllocationRow from(AssetTypeValueDto value) {
            return new AllocationRow(value.assetType(), DesktopFormatters.money(value.value()), DesktopFormatters.percent(value.allocation()));
        }

        public String getAssetType() {
            return assetType;
        }

        public String getValue() {
            return value;
        }

        public String getShare() {
            return share;
        }
    }
}
