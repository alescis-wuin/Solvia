package fr.seynax.solvia.desktop.ui;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import fr.seynax.solvia.desktop.api.ApiDtos.NetWorthDto;
import fr.seynax.solvia.desktop.api.ApiDtos.PerformanceDto;

public final class DashboardMetrics extends HBox {

    private final MetricCard total = new MetricCard("Patrimoine", "Valeur totale a la date de fin.");
    private final MetricCard grossChange = new MetricCard("Variation brute", "Evolution sans correction des versements et retraits.");
    private final MetricCard adjustedGain = new MetricCard("Gain corrige", "Evolution corrigee des flux externes.");

    public DashboardMetrics() {
        getStyleClass().add("metric-grid");
        setSpacing(14);
        getChildren().addAll(total, grossChange, adjustedGain);
        HBox.setHgrow(total, Priority.ALWAYS);
        HBox.setHgrow(grossChange, Priority.ALWAYS);
        HBox.setHgrow(adjustedGain, Priority.ALWAYS);
        clear();
    }

    public void update(NetWorthDto netWorth, PerformanceDto performance) {
        total.setValue(DesktopFormatters.money(netWorth.total()));
        grossChange.setValue(DesktopFormatters.signedMoney(performance.grossChange()) + " / "
                + DesktopFormatters.percent(performance.grossChangePercentage()));
        adjustedGain.setValue(DesktopFormatters.signedMoney(performance.flowAdjustedGain()) + " / "
                + DesktopFormatters.percent(performance.flowAdjustedGainPercentage()));
    }

    public void clear() {
        total.setValue("--");
        grossChange.setValue("--");
        adjustedGain.setValue("--");
    }
}
