package fr.seynax.solvia.desktop.ui;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import javafx.scene.layout.VBox;
import javafx.scene.shape.Line;
import javafx.util.StringConverter;

import fr.seynax.solvia.desktop.api.ApiDtos.SeriesPointDto;
import fr.seynax.solvia.desktop.ui.DashboardChartAxisSelector.AxisOverrides;
import fr.seynax.solvia.desktop.ui.DashboardChartAxisSelector.DataBounds;

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
    private final DashboardChartAxisSelector axisSelector;
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
        super("Historique", "Évolution du patrimoine sur la période sélectionnée.", parts.selector(), parts.layer(), parts.summary());
        this.xAxis = parts.xAxis();
        this.yAxis = parts.yAxis();
        this.chart = parts.chart();
        this.axisSelector = parts.selector();
        this.chartLayer = parts.layer();
        this.verticalGuide = parts.verticalGuide();
        this.horizontalGuide = parts.horizontalGuide();
        this.hoverLabel = parts.hoverLabel();
        this.summary = parts.summary();
        this.axisSelector.setOnAxisChanged(() -> configureAxes(displayedPoints));
        getChildren().add(empty);
        VBox.setVgrow(chartLayer, Priority.ALWAYS);
        empty.show("Aucune série", "Les points apparaîtront après la saisie de valeurs.");
    }

    public void update(List<SeriesPointDto> points) {
        List<ChartPoint> sourcePoints = normalize(points);
        List<ChartPoint> chartPoints = focusOnValueChanges(sourcePoints);
        displayedPoints = chartPoints;
        axisSelector.setDataBounds(dataBounds(chartPoints));
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
        axisSelector.setDataBounds(null);
        chart.getData().clear();
        hideGuides();
        summary.setText("Aucun point affiché.");
        empty.show("Aucune série", "Les points apparaîtront après la saisie de valeurs.");
    }

    private static ChartParts createChartParts() {
        NumberAxis xAxis = new NumberAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Date / heure");
        yAxis.setLabel("Patrimoine total");
        xAxis.setForceZeroInRange(false);
        yAxis.setForceZeroInRange(false);
        xAxis.setAutoRanging(false);
        yAxis.setAutoRanging(false);
        xAxis.setMinorTickVisible(false);
        yAxis.setMinorTickVisible(true);
        yAxis.setMinorTickCount(3);

        LineChart<Number, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Historique du patrimoine");
        lineChart.setLegendVisible(false);
        lineChart.setAnimated(false);
        lineChart.setHorizontalGridLinesVisible(true);
        lineChart.setVerticalGridLinesVisible(true);
        lineChart.setAlternativeColumnFillVisible(false);
        lineChart.setAlternativeRowFillVisible(false);
        lineChart.setAccessibleText("Graphique de l'historique du patrimoine total avec axes dynamiques, dates précises et infobulle au survol.");

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

        DashboardChartAxisSelector selector = new DashboardChartAxisSelector();
        Label summary = Ui.help("Aucun point affiché.");
        summary.getStyleClass().add("chart-summary");
        return new ChartParts(xAxis, yAxis, lineChart, selector, layer, verticalGuide, horizontalGuide, hoverLabel, summary);
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
        AxisOverrides overrides = axisSelector.axisOverrides();
        double lower;
        double upper;
        long tick;
        if (points.isEmpty()) {
            lower = 0;
            upper = DAY;
            tick = DAY;
        } else {
            double min = points.stream().mapToDouble(ChartPoint::x).min().orElse(0);
            double max = points.stream().mapToDouble(ChartPoint::x).max().orElse(min);
            if (Double.compare(min, max) == 0) {
                long window = LocalTime.MIDNIGHT.equals(points.get(0).date().toLocalTime()) ? DAY : HOUR;
                lower = min - window / 2.0;
                upper = max + window / 2.0;
                tick = window;
            } else if (points.size() <= 2) {
                long duration = Math.max(SECOND, Math.round(max - min));
                lower = min;
                upper = max;
                tick = duration;
            } else {
                long duration = Math.max(1L, Math.round(max - min));
                tick = niceTimeTick(duration / 5.0);
                long padding = Math.max(tick / 2L, Math.min(duration / 20L, tick));
                lower = Math.floor((min - padding) / tick) * tick;
                upper = Math.ceil((max + padding) / tick) * tick;
            }
        }
        if (overrides.manual()) {
            if (overrides.timeMin() != null) {
                lower = epochMillis(overrides.timeMin());
            }
            if (overrides.timeMax() != null) {
                upper = epochMillis(overrides.timeMax());
            }
            if (overrides.timeStep() != null) {
                tick = Math.max(SECOND, overrides.timeStep().toMillis());
            }
        }
        if (upper <= lower) {
            upper = lower + Math.max(SECOND, tick);
        }
        xTickUnit = Math.max(SECOND, tick);
        xAxis.setLowerBound(lower);
        xAxis.setUpperBound(upper);
        xAxis.setTickUnit(xTickUnit);
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
        AxisOverrides overrides = axisSelector.axisOverrides();
        double lower;
        double upper;
        double tick;
        boolean nonNegativeSeries;
        if (points.isEmpty()) {
            lower = 0;
            upper = 1;
            tick = 1;
            nonNegativeSeries = true;
        } else {
            double min = points.stream().mapToDouble(ChartPoint::y).min().orElse(0);
            double max = points.stream().mapToDouble(ChartPoint::y).max().orElse(min);
            nonNegativeSeries = min >= 0;
            if (Double.compare(min, max) == 0) {
                double base = Math.max(1.0, Math.abs(min));
                double padding = niceNumber(base * 0.04, false);
                lower = min - padding;
                upper = max + padding;
                if (nonNegativeSeries && lower < 0) {
                    lower = 0;
                }
                tick = niceNumber(Math.max((upper - lower) / 2.0, 1.0), true);
            } else {
                double range = max - min;
                double padding = Math.max(range * 0.06, Math.abs(max) * 0.003);
                lower = min - padding;
                upper = max + padding;
                tick = niceNumber((upper - lower) / 5.0, true);
            }
        }
        if (overrides.manual()) {
            if (overrides.valueMin() != null) {
                lower = overrides.valueMin().doubleValue();
            }
            if (overrides.valueMax() != null) {
                upper = overrides.valueMax().doubleValue();
            }
            if (overrides.valueStep() != null) {
                tick = overrides.valueStep().doubleValue();
            }
        } else {
            lower = Math.floor(lower / tick) * tick;
            upper = Math.ceil(upper / tick) * tick;
            if (nonNegativeSeries && lower < 0) {
                lower = 0;
            }
        }
        tick = Math.max(tick, 0.000001);
        if (upper <= lower) {
            upper = lower + tick;
        }
        yTickUnit = tick;
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

        hoverLabel.setText("Patrimoine total affiché\n" + formatPreciseDate(point.date()) + "\n" + formatMoney(point));
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
        Tooltip tooltip = new Tooltip("Patrimoine total affiché\n" + formatPreciseDate(point.date()) + "\n" + formatMoney(point));
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
        BigDecimal roundedAmount = point.value().amount().setScale(2, RoundingMode.HALF_UP);
        double x = point.valueDate().atZone(DISPLAY_ZONE).toInstant().toEpochMilli();
        double y = roundedAmount.doubleValue();
        return new ChartPoint(point, point.valueDate(), x, y, roundedAmount);
    }

    private List<ChartPoint> focusOnValueChanges(List<ChartPoint> points) {
        if (points.size() <= 2) {
            return points;
        }
        List<ChartPoint> changes = new ArrayList<>();
        for (int index = 1; index < points.size(); index++) {
            ChartPoint previous = points.get(index - 1);
            ChartPoint current = points.get(index);
            if (!sameAmount(previous, current)) {
                addIfNew(changes, previous);
                addIfNew(changes, current);
            }
        }
        if (changes.isEmpty()) {
            return List.of(points.get(0), points.get(points.size() - 1));
        }
        return List.copyOf(changes);
    }

    private void addIfNew(List<ChartPoint> points, ChartPoint candidate) {
        if (points.isEmpty()) {
            points.add(candidate);
            return;
        }
        ChartPoint last = points.get(points.size() - 1);
        if (Double.compare(last.x(), candidate.x()) != 0 || last.roundedAmount().compareTo(candidate.roundedAmount()) != 0) {
            points.add(candidate);
        }
    }

    private boolean sameAmount(ChartPoint first, ChartPoint second) {
        return first.roundedAmount().compareTo(second.roundedAmount()) == 0;
    }

    private DataBounds dataBounds(List<ChartPoint> points) {
        if (points == null || points.isEmpty()) {
            return null;
        }
        LocalDateTime timeMin = points.stream().map(ChartPoint::date).min(Comparator.naturalOrder()).orElse(null);
        LocalDateTime timeMax = points.stream().map(ChartPoint::date).max(Comparator.naturalOrder()).orElse(null);
        BigDecimal valueMin = points.stream().map(ChartPoint::roundedAmount).min(Comparator.naturalOrder()).orElse(null);
        BigDecimal valueMax = points.stream().map(ChartPoint::roundedAmount).max(Comparator.naturalOrder()).orElse(null);
        return new DataBounds(timeMin, timeMax, valueMin, valueMax);
    }

    private String summaryText(int sourceCount, int displayedCount) {
        if (sourceCount == displayedCount) {
            return displayedCount + " point" + plural(displayedCount) + " affiché" + plural(displayedCount) + ".";
        }
        int hidden = sourceCount - displayedCount;
        return displayedCount + " point" + plural(displayedCount) + " utile" + plural(displayedCount)
                + " affiché" + plural(displayedCount) + " — " + hidden + " point" + plural(hidden)
                + " sans changement visible masqué" + plural(hidden) + ".";
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

    private String formatMoney(ChartPoint point) {
        NumberFormat format = NumberFormat.getNumberInstance(DISPLAY_LOCALE);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        String currency = point.source().value().currencyCode() == null ? "" : " " + point.source().value().currencyCode();
        return format.format(point.roundedAmount()) + currency;
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

    private long epochMillis(LocalDateTime dateTime) {
        return dateTime.atZone(DISPLAY_ZONE).toInstant().toEpochMilli();
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    private String plural(int count) {
        return count > 1 ? "s" : "";
    }

    private record ChartPoint(SeriesPointDto source, LocalDateTime date, double x, double y, BigDecimal roundedAmount) {
    }

    private record ChartParts(
            NumberAxis xAxis,
            NumberAxis yAxis,
            LineChart<Number, Number> chart,
            DashboardChartAxisSelector selector,
            StackPane layer,
            Line verticalGuide,
            Line horizontalGuide,
            Label hoverLabel,
            Label summary
    ) {
    }
}
