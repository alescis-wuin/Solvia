package fr.seynax.solvia.desktop.ui;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Line;
import javafx.util.StringConverter;

import fr.seynax.solvia.desktop.api.ApiDtos.SeriesPointDto;

public final class DashboardChartCard extends SectionCard {

    private static final Locale DISPLAY_LOCALE = Locale.FRANCE;
    private static final ZoneId DISPLAY_ZONE = ZoneId.systemDefault();
    private static final long SECOND = 1_000L;
    private static final long MINUTE = 60L * SECOND;
    private static final long HOUR = 60L * MINUTE;
    private static final long DAY = 24L * HOUR;
    private static final long[] TIME_TICK_UNITS = {
            SECOND,
            5L * SECOND,
            15L * SECOND,
            30L * SECOND,
            MINUTE,
            5L * MINUTE,
            15L * MINUTE,
            30L * MINUTE,
            HOUR,
            6L * HOUR,
            12L * HOUR,
            DAY,
            2L * DAY,
            3L * DAY,
            7L * DAY,
            14L * DAY,
            30L * DAY,
            90L * DAY,
            180L * DAY,
            365L * DAY
    };
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy").withLocale(DISPLAY_LOCALE);
    private static final DateTimeFormatter DATE_SHORT_FORMATTER = DateTimeFormatter.ofPattern("dd/MM").withLocale(DISPLAY_LOCALE);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM HH:mm").withLocale(DISPLAY_LOCALE);
    private static final DateTimeFormatter DATE_TIME_SECONDS_FORMATTER = DateTimeFormatter.ofPattern("dd/MM HH:mm:ss").withLocale(DISPLAY_LOCALE);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss").withLocale(DISPLAY_LOCALE);
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy").withLocale(DISPLAY_LOCALE);
    private static final DateTimeFormatter YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy").withLocale(DISPLAY_LOCALE);

    private final NumberAxis xAxis;
    private final NumberAxis yAxis;
    private final LineChart<Number, Number> chart;
    private final StackPane chartLayer;
    private final Line verticalGuide;
    private final Line horizontalGuide;
    private final Label hoverLabel;
    private final Label summary;
    private final EmptyState empty = new EmptyState();
    private List<ChartPoint> displayedPoints = List.of();
    private long xTickUnit = DAY;
    private double yTickUnit = 1.0;

    public DashboardChartCard() {
        this(createChartParts());
    }

    private DashboardChartCard(ChartParts parts) {
        super("Historique", "Évolution du patrimoine sur la période sélectionnée.", parts.layer(), parts.summary());
        this.xAxis = parts.xAxis();
        this.yAxis = parts.yAxis();
        this.chart = parts.chart();
        this.chartLayer = parts.layer();
        this.verticalGuide = parts.verticalGuide();
        this.horizontalGuide = parts.horizontalGuide();
        this.hoverLabel = parts.hoverLabel();
        this.summary = parts.summary();
        getChildren().add(empty);
        VBox.setVgrow(chartLayer, Priority.ALWAYS);
        empty.show("Aucune série", "Les points apparaîtront après la saisie de valeurs.");
    }

    public void update(List<SeriesPointDto> points) {
        List<ChartPoint> sourcePoints = normalize(points);
        List<ChartPoint> chartPoints = compressPlateaus(sourcePoints);
        displayedPoints = chartPoints;
        hideGuides();
        configureAxes(chartPoints);

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        for (ChartPoint point : chartPoints) {
            XYChart.Data<Number, Number> data = new XYChart.Data<>(point.x(), point.y());
            data.nodeProperty().addListener((observable, oldNode, newNode) -> installTooltip(newNode, point));
            series.getData().add(data);
        }
        chart.setCreateSymbols(chartPoints.size() <= 80);
        chart.getData().setAll(series);
        for (int index = 0; index < series.getData().size(); index++) {
            installTooltip(series.getData().get(index).getNode(), chartPoints.get(index));
        }

        if (chartPoints.isEmpty()) {
            empty.show("Aucune série", "Aucune valeur n'est disponible sur cette période.");
            summary.setText("Aucun point affiché.");
        } else {
            empty.hide();
            summary.setText(summaryText(sourcePoints.size(), chartPoints.size()));
        }
    }

    public void clear() {
        displayedPoints = List.of();
        chart.getData().clear();
        hideGuides();
        summary.setText("Aucun point affiché.");
        empty.show("Aucune série", "Les points apparaîtront après la saisie de valeurs.");
    }

    private static ChartParts createChartParts() {
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Date / heure");
        yAxis.setLabel("Patrimoine");
        xAxis.setForceZeroInRange(false);
        yAxis.setForceZeroInRange(false);
        xAxis.setAutoRanging(false);
        yAxis.setAutoRanging(false);
        xAxis.setMinorTickVisible(false);
        yAxis.setMinorTickCount(4);

        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Historique du patrimoine");
        lineChart.setLegendVisible(false);
        lineChart.setAnimated(false);
        lineChart.setHorizontalGridLinesVisible(true);
        lineChart.setVerticalGridLinesVisible(true);
        lineChart.setAlternativeColumnFillVisible(false);
        lineChart.setAlternativeRowFillVisible(false);
        lineChart.setAccessibleText("Graphique de l'historique du patrimoine avec axes dynamiques, dates précises et infobulle au survol.");

        Line verticalGuide = guideLine();
        Line horizontalGuide = guideLine();
        Label hoverLabel = Ui.label("—", "chart-hover-label");
        hoverLabel.setManaged(false);
        hoverLabel.setMouseTransparent(true);
        hoverLabel.setVisible(false);

        StackPane layer = new StackPane(lineChart, verticalGuide, horizontalGuide, hoverLabel);
        layer.getStyleClass().add("dashboard-chart-layer");
        layer.addEventHandler(MouseEvent.MOUSE_MOVED, event -> {
            DashboardChartCard card = findCard(layer);
            if (card != null) {
                card.handleMouseMoved(event);
            }
        });
        layer.addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
            DashboardChartCard card = findCard(layer);
            if (card != null) {
                card.hideGuides();
            }
        });

        Label summary = Ui.help("Aucun point affiché.");
        summary.getStyleClass().add("chart-summary");
        return new ChartParts(xAxis, yAxis, lineChart, layer, verticalGuide, horizontalGuide, hoverLabel, summary);
    }

    private static Line guideLine() {
        Line line = new Line();
        line.getStyleClass().add("chart-hover-line");
        line.getStrokeDashArray().setAll(6.0, 5.0);
        line.setManaged(false);
        line.setMouseTransparent(true);
        line.setVisible(false);
        return line;
    }

    private static DashboardChartCard findCard(Node node) {
        Node current = node;
        while (current != null) {
            if (current instanceof DashboardChartCard card) {
                return card;
            }
            current = current.getParent();
        }
        return null;
    }

    private void configureAxes(List<ChartPoint> points) {
        configureXAxis(points);
        configureYAxis(points);
    }

    private void configureXAxis(List<ChartPoint> points) {
        if (points.isEmpty()) {
            xAxis.setLowerBound(0);
            xAxis.setUpperBound(DAY);
            xAxis.setTickUnit(DAY);
            return;
        }
        double min = points.stream().mapToDouble(ChartPoint::x).min().orElse(0);
        double max = points.stream().mapToDouble(ChartPoint::x).max().orElse(min);
        if (Double.compare(min, max) == 0) {
            long window = LocalTime.MIDNIGHT.equals(points.get(0).date().toLocalTime()) ? DAY : HOUR;
            xTickUnit = Math.max(SECOND, window / 2L);
            xAxis.setLowerBound(min - window / 2.0);
            xAxis.setUpperBound(max + window / 2.0);
            xAxis.setTickUnit(xTickUnit);
        } else {
            long duration = Math.max(1L, Math.round(max - min));
            xTickUnit = niceTimeTick(duration / 6.0);
            long padding = Math.max(xTickUnit / 2L, Math.min(duration / 20L, xTickUnit));
            double lower = Math.floor((min - padding) / xTickUnit) * xTickUnit;
            double upper = Math.ceil((max + padding) / xTickUnit) * xTickUnit;
            if (Double.compare(lower, upper) == 0) {
                upper = lower + xTickUnit;
            }
            xAxis.setLowerBound(lower);
            xAxis.setUpperBound(upper);
            xAxis.setTickUnit(xTickUnit);
        }
        xAxis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number value) {
                return formatTickDate(value == null ? 0L : value.longValue());
            }

            @Override
            public Number fromString(String string) {
                return 0;
            }
        });
    }

    private void configureYAxis(List<ChartPoint> points) {
        if (points.isEmpty()) {
            yAxis.setLowerBound(0);
            yAxis.setUpperBound(1);
            yAxis.setTickUnit(1);
            return;
        }
        double min = points.stream().mapToDouble(ChartPoint::y).min().orElse(0);
        double max = points.stream().mapToDouble(ChartPoint::y).max().orElse(min);
        double lower;
        double upper;
        if (Double.compare(min, max) == 0) {
            double base = Math.max(1.0, Math.abs(min));
            double padding = niceNumber(base * 0.05, false);
            lower = min - padding;
            upper = max + padding;
        } else {
            double range = max - min;
            double padding = Math.max(range * 0.08, Math.abs(max) * 0.005);
            lower = min - padding;
            upper = max + padding;
        }
        yTickUnit = niceNumber((upper - lower) / 6.0, true);
        lower = Math.floor(lower / yTickUnit) * yTickUnit;
        upper = Math.ceil(upper / yTickUnit) * yTickUnit;
        if (Double.compare(lower, upper) == 0) {
            upper = lower + yTickUnit;
        }
        yAxis.setLowerBound(lower);
        yAxis.setUpperBound(upper);
        yAxis.setTickUnit(yTickUnit);
        yAxis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number value) {
                return formatAmountTick(value == null ? 0 : value.doubleValue());
            }

            @Override
            public Number fromString(String string) {
                return 0;
            }
        });
    }

    private void handleMouseMoved(MouseEvent event) {
        if (displayedPoints.isEmpty()) {
            hideGuides();
            return;
        }
        Node plot = chart.lookup(".chart-plot-background");
        if (plot == null) {
            hideGuides();
            return;
        }
        Bounds plotBounds = chartLayer.sceneToLocal(plot.localToScene(plot.getBoundsInLocal()));
        Point2D mouseInLayer = chartLayer.sceneToLocal(event.getSceneX(), event.getSceneY());
        if (!plotBounds.contains(mouseInLayer)) {
            hideGuides();
            return;
        }
        Point2D mouseInXAxis = xAxis.sceneToLocal(event.getSceneX(), event.getSceneY());
        Number valueForDisplay = xAxis.getValueForDisplay(mouseInXAxis.getX());
        if (valueForDisplay == null) {
            hideGuides();
            return;
        }
        ChartPoint nearest = nearestPoint(valueForDisplay.doubleValue());
        showGuides(nearest, plotBounds);
    }

    private ChartPoint nearestPoint(double xValue) {
        ChartPoint nearest = displayedPoints.get(0);
        double bestDistance = Math.abs(nearest.x() - xValue);
        for (int index = 1; index < displayedPoints.size(); index++) {
            ChartPoint candidate = displayedPoints.get(index);
            double distance = Math.abs(candidate.x() - xValue);
            if (distance < bestDistance) {
                nearest = candidate;
                bestDistance = distance;
            }
        }
        return nearest;
    }

    private void showGuides(ChartPoint point, Bounds plotBounds) {
        Point2D xScene = xAxis.localToScene(xAxis.getDisplayPosition(point.x()), 0);
        Point2D yScene = yAxis.localToScene(0, yAxis.getDisplayPosition(point.y()));
        Point2D position = chartLayer.sceneToLocal(xScene.getX(), yScene.getY());
        if (!plotBounds.contains(position)) {
            hideGuides();
            return;
        }

        verticalGuide.setStartX(position.getX());
        verticalGuide.setEndX(position.getX());
        verticalGuide.setStartY(plotBounds.getMinY());
        verticalGuide.setEndY(plotBounds.getMaxY());

        horizontalGuide.setStartX(plotBounds.getMinX());
        horizontalGuide.setEndX(plotBounds.getMaxX());
        horizontalGuide.setStartY(position.getY());
        horizontalGuide.setEndY(position.getY());

        hoverLabel.setText(formatPreciseDate(point.date()) + "\n" + DesktopFormatters.money(point.source().value()));
        hoverLabel.autosize();
        double labelX = clamp(position.getX() + 12, plotBounds.getMinX() + 6, plotBounds.getMaxX() - hoverLabel.prefWidth(-1) - 6);
        double labelY = clamp(position.getY() - hoverLabel.prefHeight(-1) - 12, plotBounds.getMinY() + 6, plotBounds.getMaxY() - hoverLabel.prefHeight(-1) - 6);
        hoverLabel.setLayoutX(labelX);
        hoverLabel.setLayoutY(labelY);

        verticalGuide.setVisible(true);
        horizontalGuide.setVisible(true);
        hoverLabel.setVisible(true);
    }

    private void hideGuides() {
        verticalGuide.setVisible(false);
        horizontalGuide.setVisible(false);
        hoverLabel.setVisible(false);
    }

    private void installTooltip(Node node, ChartPoint point) {
        if (node == null || point == null) {
            return;
        }
        Tooltip tooltip = new Tooltip(formatPreciseDate(point.date()) + "\n" + DesktopFormatters.money(point.source().value()));
        tooltip.getStyleClass().add("chart-tooltip");
        Tooltip.install(node, tooltip);
    }

    private List<ChartPoint> normalize(List<SeriesPointDto> points) {
        if (points == null || points.isEmpty()) {
            return List.of();
        }
        List<ChartPoint> normalized = points.stream()
                .filter(point -> point != null && point.valueDate() != null && point.value() != null && point.value().amount() != null)
                .sorted(Comparator.comparing(SeriesPointDto::valueDate))
                .map(this::chartPoint)
                .toList();
        if (normalized.isEmpty()) {
            return List.of();
        }
        List<ChartPoint> deduplicated = new ArrayList<>();
        for (ChartPoint point : normalized) {
            int lastIndex = deduplicated.size() - 1;
            if (lastIndex >= 0 && Double.compare(deduplicated.get(lastIndex).x(), point.x()) == 0) {
                deduplicated.set(lastIndex, point);
            } else {
                deduplicated.add(point);
            }
        }
        return List.copyOf(deduplicated);
    }

    private ChartPoint chartPoint(SeriesPointDto point) {
        double x = point.valueDate().atZone(DISPLAY_ZONE).toInstant().toEpochMilli();
        double y = point.value().amount().doubleValue();
        return new ChartPoint(point, point.valueDate(), x, y);
    }

    private List<ChartPoint> compressPlateaus(List<ChartPoint> points) {
        if (points.size() <= 2) {
            return points;
        }
        List<ChartPoint> compressed = new ArrayList<>();
        compressed.add(points.get(0));
        for (int index = 1; index < points.size() - 1; index++) {
            ChartPoint previous = points.get(index - 1);
            ChartPoint current = points.get(index);
            ChartPoint next = points.get(index + 1);
            if (!sameAmount(previous, current) || !sameAmount(current, next)) {
                compressed.add(current);
            }
        }
        compressed.add(points.get(points.size() - 1));
        return List.copyOf(compressed);
    }

    private boolean sameAmount(ChartPoint first, ChartPoint second) {
        BigDecimal firstAmount = first.source().value().amount();
        BigDecimal secondAmount = second.source().value().amount();
        return firstAmount.compareTo(secondAmount) == 0;
    }

    private String summaryText(int sourceCount, int displayedCount) {
        if (sourceCount == displayedCount) {
            return displayedCount + " point" + plural(displayedCount) + " affiché" + plural(displayedCount) + ".";
        }
        int hidden = sourceCount - displayedCount;
        return displayedCount + " point" + plural(displayedCount) + " affiché" + plural(displayedCount)
                + " — " + hidden + " point" + plural(hidden) + " redondant" + plural(hidden) + " masqué" + plural(hidden) + " sur plateau inchangé.";
    }

    private String formatTickDate(long epochMillis) {
        LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), DISPLAY_ZONE);
        if (xTickUnit < MINUTE) {
            return TIME_FORMATTER.format(date);
        }
        if (xTickUnit < DAY) {
            return DATE_TIME_FORMATTER.format(date);
        }
        if (xTickUnit < 90L * DAY) {
            return DATE_SHORT_FORMATTER.format(date);
        }
        if (xTickUnit < 365L * DAY) {
            return MONTH_FORMATTER.format(date);
        }
        return YEAR_FORMATTER.format(date);
    }

    private String formatPreciseDate(LocalDateTime date) {
        if (date == null) {
            return "—";
        }
        if (LocalTime.MIDNIGHT.equals(date.toLocalTime())) {
            return DATE_FORMATTER.format(date);
        }
        if (date.getSecond() == 0) {
            return DATE_TIME_FORMATTER.format(date);
        }
        return DATE_TIME_SECONDS_FORMATTER.format(date);
    }

    private String formatAmountTick(double value) {
        NumberFormat format = NumberFormat.getNumberInstance(DISPLAY_LOCALE);
        int decimals = yTickUnit >= 1 ? 0 : decimalCount(yTickUnit);
        format.setMinimumFractionDigits(0);
        format.setMaximumFractionDigits(Math.min(6, decimals));
        return format.format(value);
    }

    private int decimalCount(double value) {
        double current = Math.abs(value);
        int decimals = 0;
        while (current > 0 && current < 1 && decimals < 6) {
            current *= 10;
            decimals++;
        }
        return decimals;
    }

    private long niceTimeTick(double roughUnit) {
        for (long unit : TIME_TICK_UNITS) {
            if (unit >= roughUnit) {
                return unit;
            }
        }
        return TIME_TICK_UNITS[TIME_TICK_UNITS.length - 1];
    }

    private double niceNumber(double value, boolean round) {
        if (!Double.isFinite(value) || value <= 0) {
            return 1.0;
        }
        double exponent = Math.floor(Math.log10(value));
        double fraction = value / Math.pow(10, exponent);
        double niceFraction;
        if (round) {
            if (fraction < 1.5) {
                niceFraction = 1;
            } else if (fraction < 3) {
                niceFraction = 2;
            } else if (fraction < 7) {
                niceFraction = 5;
            } else {
                niceFraction = 10;
            }
        } else {
            if (fraction <= 1) {
                niceFraction = 1;
            } else if (fraction <= 2) {
                niceFraction = 2;
            } else if (fraction <= 5) {
                niceFraction = 5;
            } else {
                niceFraction = 10;
            }
        }
        return niceFraction * Math.pow(10, exponent);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    private String plural(int count) {
        return count > 1 ? "s" : "";
    }

    private record ChartPoint(SeriesPointDto source, LocalDateTime date, double x, double y) {
    }

    private record ChartParts(
            NumberAxis xAxis,
            NumberAxis yAxis,
            LineChart<Number, Number> chart,
            StackPane layer,
            Line verticalGuide,
            Line horizontalGuide,
            Label hoverLabel,
            Label summary
    ) {
    }
}
