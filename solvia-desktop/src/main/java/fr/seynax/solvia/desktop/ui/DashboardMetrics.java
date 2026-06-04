package fr.seynax.solvia.desktop.ui;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import fr.seynax.solvia.desktop.api.ApiDtos.NetWorthDto;
import fr.seynax.solvia.desktop.api.ApiDtos.PerformanceDto;

public final class DashboardMetrics extends HBox {

    private final MetricCard total = new MetricCard("Patrimoine", "Valeur totale à la date de fin.");
    private final MetricCard grossChange = new MetricCard("Variation brute", "Évolution sans correction des versements et retraits.");
    private final MetricCard externalFlow = new MetricCard("Flux externe net", "Versements moins retraits et transferts externes.");
    private final MetricCard adjustedGain = new MetricCard("Gain corrigé", "Évolution corrigée des flux externes.");

    public DashboardMetrics() {
        getStyleClass().add("metric-grid");
        setSpacing(14);
        getChildren().addAll(total, grossChange, externalFlow, adjustedGain);
        HBox.setHgrow(total, Priority.ALWAYS);
        HBox.setHgrow(grossChange, Priority.ALWAYS);
        HBox.setHgrow(externalFlow, Priority.ALWAYS);
        HBox.setHgrow(adjustedGain, Priority.ALWAYS);
        clear();
    }

    public void update(NetWorthDto netWorth, PerformanceDto performance) {
        total.setValue(DesktopFormatters.money(netWorth.total()));
        total.setCaption("Valorisation au " + DesktopFormatters.date(netWorth.valueDate()) + ".");
        grossChange.setValue(DesktopFormatters.signedMoney(performance.grossChange()) + " / "
                + DesktopFormatters.percent(performance.grossChangePercentage()));
        grossChange.setCaption("Variation patrimoniale brute sur la période.");
        externalFlow.setValue(DesktopFormatters.signedMoney(performance.netExternalFlow()));
        externalFlow.setCaption("Capital ajouté ou retiré entre les deux dates.");
        adjustedGain.setValue(DesktopFormatters.signedMoney(performance.flowAdjustedGain()) + " / "
                + DesktopFormatters.percent(performance.flowAdjustedGainPercentage()));
        adjustedGain.setCaption("Gain ou perte après neutralisation des flux externes.");
    }

    public void clear() {
        total.setValue("--");
        total.setCaption("Valeur totale à la date de fin.");
        grossChange.setValue("--");
        grossChange.setCaption("Évolution sans correction des versements et retraits.");
        externalFlow.setValue("--");
        externalFlow.setCaption("Versements moins retraits et transferts externes.");
        adjustedGain.setValue("--");
        adjustedGain.setCaption("Évolution corrigée des flux externes.");
    }
}
