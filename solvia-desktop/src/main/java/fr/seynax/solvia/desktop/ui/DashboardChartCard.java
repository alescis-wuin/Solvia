package fr.seynax.solvia.desktop.ui;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import fr.seynax.solvia.desktop.api.ApiDtos.SeriesPointDto;

public final class DashboardChartCard extends SectionCard {

    private final LineChart<Number, Number> chart;

    public DashboardChartCard() {
        this(createChart());
    }

    private DashboardChartCard(LineChart<Number, Number> chart) {
        super("Historique", "Evolution du patrimoine sur la periode selectionnee.", chart);
        this.chart = chart;
        VBox.setVgrow(chart, Priority.ALWAYS);
    }

    public void update(LocalDate start, List<SeriesPointDto> points) {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        for (SeriesPointDto point : points) {
            long day = ChronoUnit.DAYS.between(start, point.valueDate());
            BigDecimal amount = point.value().amount();
            series.getData().add(new XYChart.Data<>(day, amount.doubleValue()));
        }
        chart.getData().setAll(series);
    }

    public void clear() {
        chart.getData().clear();
    }

    private static LineChart<Number, Number> createChart() {
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Jours");
        yAxis.setLabel("Patrimoine");
        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Historique du patrimoine");
        lineChart.setLegendVisible(false);
        lineChart.setAnimated(false);
        lineChart.setAccessibleText("Graphique de l'historique du patrimoine.");
        return lineChart;
    }
}
