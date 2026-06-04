package fr.seynax.solvia.desktop.ui;

import java.util.List;

import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import fr.seynax.solvia.desktop.api.ApiDtos.NetWorthDto;
import fr.seynax.solvia.desktop.api.ApiDtos.SeriesPointDto;

public final class DashboardQualityCard extends SectionCard {

    private final Label period = Ui.label("--", "monospace");
    private final Label valuationDate = Ui.label("--", "monospace");
    private final Label seriesPoints = Ui.label("--", "monospace");
    private final Label valuedAccounts = Ui.label("--", "monospace");
    private final Label allocationRows = Ui.label("--", "monospace");
    private final Label chartMode = Ui.label("--", "monospace");

    public DashboardQualityCard() {
        super("Qualite des donnees", "Controle rapide de la periode, de la fraicheur et de la densite des donnees affichees.");
        GridPane grid = Ui.style(new GridPane(), "dashboard-quality-grid");
        grid.setHgap(18);
        grid.setVgap(8);
        addRow(grid, 0, "Periode", period);
        addRow(grid, 1, "Valorisation", valuationDate);
        addRow(grid, 2, "Points", seriesPoints);
        addRow(grid, 3, "Comptes valorises", valuedAccounts);
        addRow(grid, 4, "Classes d'actifs", allocationRows);
        addRow(grid, 5, "Lecture", chartMode);
        getChildren().add(grid);
        clear();
    }

    public void update(NetWorthDto netWorth, List<SeriesPointDto> points, DashboardFilters filters) {
        int accountCount = netWorth.accounts() == null ? 0 : netWorth.accounts().size();
        int allocationCount = netWorth.allocation() == null ? 0 : netWorth.allocation().size();
        int pointCount = points == null ? 0 : points.size();
        period.setText(DesktopFormatters.period(filters.from(), filters.to()));
        valuationDate.setText(DesktopFormatters.date(netWorth.valueDate()));
        seriesPoints.setText(DesktopFormatters.count(pointCount, "point", "points"));
        valuedAccounts.setText(DesktopFormatters.count(accountCount, "compte", "comptes"));
        allocationRows.setText(DesktopFormatters.count(allocationCount, "classe", "classes"));
        chartMode.setText(DesktopFormatters.aggregation(filters.aggregation()) + " / " + DesktopFormatters.bucket(filters.bucket()));
    }

    public void clear() {
        period.setText("--");
        valuationDate.setText("--");
        seriesPoints.setText("--");
        valuedAccounts.setText("--");
        allocationRows.setText("--");
        chartMode.setText("--");
    }

    private void addRow(GridPane grid, int row, String label, Label value) {
        grid.add(Ui.label(label, "field-label"), 0, row);
        grid.add(value, 1, row);
    }
}
