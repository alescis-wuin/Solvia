package fr.seynax.solvia.desktop.ui;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import fr.seynax.solvia.desktop.api.ApiDtos.AssetTypeValueDto;
import fr.seynax.solvia.desktop.api.ApiDtos.MoneyDto;

public final class DashboardAllocationCard extends SectionCard {

    private final PieChart chart = new PieChart();
    private final VBox legend = new VBox(12);
    private final HBox content = new HBox(20, chart, legend);
    private final EmptyState empty = new EmptyState();

    public DashboardAllocationCard() {
        super("Répartition", "Répartition par classe d'actifs.");
        chart.getStyleClass().add("dashboard-allocation-chart");
        chart.setLegendVisible(false);
        chart.setLabelsVisible(false);
        chart.setClockwise(true);
        chart.setStartAngle(90);
        chart.setMinSize(170, 170);
        chart.setPrefSize(190, 190);
        legend.getStyleClass().add("dashboard-allocation-legend");
        content.getStyleClass().add("dashboard-allocation-content");
        HBox.setHgrow(legend, Priority.ALWAYS);
        getChildren().addAll(empty, content);
        clear();
    }

    public void update(List<AssetTypeValueDto> allocation) {
        List<AllocationRow> rows = allocation == null ? List.of() : allocation.stream()
                .map(AllocationRow::from)
                .filter(row -> row.numericValue().signum() > 0)
                .sorted(Comparator.comparing(AllocationRow::numericValue).reversed())
                .toList();
        chart.getData().setAll(rows.stream()
                .map(row -> new PieChart.Data(row.assetType(), row.numericValue().doubleValue()))
                .toList());
        legend.getChildren().setAll(rows.stream().map(this::legendRow).toList());
        if (rows.isEmpty()) {
            empty.show("Aucune allocation", "Aucune répartition n'est disponible sur cette période.");
            content.setVisible(false);
            content.setManaged(false);
        } else {
            empty.hide();
            content.setVisible(true);
            content.setManaged(true);
        }
    }

    public void clear() {
        chart.getData().clear();
        legend.getChildren().clear();
        content.setVisible(false);
        content.setManaged(false);
        empty.show("Aucune allocation", "L'allocation apparaîtra après la saisie de valeurs.");
    }

    private HBox legendRow(AllocationRow row) {
        Region dot = new Region();
        dot.getStyleClass().add("allocation-dot");
        Label name = Ui.label(row.assetType(), "allocation-name");
        Label value = Ui.style(Ui.label(row.value(), "allocation-value"), "monospace");
        Label share = Ui.style(Ui.label(row.share(), "allocation-share"), "monospace");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox line = new HBox(10, dot, name, spacer, share, value);
        line.getStyleClass().add("allocation-row");
        return line;
    }

    public static final class AllocationRow {
        private final String assetType;
        private final String value;
        private final String share;
        private final BigDecimal numericValue;

        private AllocationRow(String assetType, String value, String share, BigDecimal numericValue) {
            this.assetType = assetType;
            this.value = value;
            this.share = share;
            this.numericValue = numericValue == null ? BigDecimal.ZERO : numericValue;
        }

        static AllocationRow from(AssetTypeValueDto value) {
            MoneyDto money = value.value();
            BigDecimal amount = money == null || money.amount() == null ? BigDecimal.ZERO : money.amount();
            return new AllocationRow(
                    DesktopFormatters.assetType(value.assetType()),
                    DesktopFormatters.money(value.value()),
                    DesktopFormatters.percent(value.allocation()),
                    amount.abs()
            );
        }

        public String assetType() {
            return assetType;
        }

        public String value() {
            return value;
        }

        public String share() {
            return share;
        }

        public BigDecimal numericValue() {
            return numericValue;
        }
    }
}
