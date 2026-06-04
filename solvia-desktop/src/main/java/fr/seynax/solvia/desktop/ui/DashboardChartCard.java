package fr.seynax.solvia.desktop.ui;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import fr.seynax.solvia.desktop.api.ApiDtos.SeriesPointDto;

public final class DashboardChartCard extends SectionCard {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM");

    private final LineChart<String, Number> chart;
    private final EmptyState empty = new EmptyState();

    public DashboardChartCard() {
        this(createChart());
    }

    private DashboardChartCard(LineChart<String, Number> chart) {
        super("Historique", "Evolution du patrimoine sur la periode selectionnee.", chart);
        this.chart = chart;
        getChildren().add(empty);
        VBox.setVgrow(chart, Priority.ALWAYS);
        empty.show("Aucune serie", "Les points apparaitront apres la saisie de valeurs.");
    }

    public void update(List<SeriesPointDto> points) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (SeriesPointDto point : points) {
            BigDecimal amount = point.value().amount();
            XYChart.Data<String, Number> data = new XYChart.Data<>(DATE_FORMATTER.format(point.valueDate()), amount);
            data.nodeProperty().addListener((observable, oldNode, newNode) -> installTooltip(newNode, point));
            series.getData().add(data);
        }
        chart.getData().setAll(series);
        for (int index = 0; index < series.getData().size(); index++) {
            installTooltip(series.getData().get(index).getNode(), points.get(index));
        }
        if (points.isEmpty()) {
            empty.show("Aucune serie", "Aucune valeur n'est disponible sur cette periode.");
        } else {
            empty.hide();
        }
    }

    public void clear() {
        chart.getData().clear();
        empty.show("Aucune serie", "Les points apparaitront apres la saisie de valeurs.");
    }

    private void installTooltip(Node node, SeriesPointDto point) {
        if (node == null || point == null) {
            return;
        }
        Tooltip tooltip = new Tooltip(point.valueDate() + "\n" + DesktopFormatters.money(point.value()));
        tooltip.getStyleClass().add("chart-tooltip");
        Tooltip.install(node, tooltip);
    }

    private static LineChart<String, Number> createChart() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Date");
        yAxis.setLabel("Patrimoine");
        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Historique du patrimoine");
        lineChart.setLegendVisible(false);
        lineChart.setAnimated(false);
        lineChart.setAccessibleText("Graphique de l'historique du patrimoine avec dates reelles.");
        return lineChart;
    }
}
