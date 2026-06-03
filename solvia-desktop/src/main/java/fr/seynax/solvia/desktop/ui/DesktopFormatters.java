package fr.seynax.solvia.desktop.ui;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.CompletionException;

import fr.seynax.solvia.desktop.api.ApiDtos.MoneyDto;
import fr.seynax.solvia.desktop.api.ApiDtos.PercentageDto;

public final class DesktopFormatters {

    private static final Locale DISPLAY_LOCALE = Locale.FRANCE;

    private DesktopFormatters() {
    }

    public static String money(MoneyDto money) {
        if (money == null || money.amount() == null || money.currencyCode() == null) {
            return "—";
        }
        NumberFormat format = NumberFormat.getNumberInstance(DISPLAY_LOCALE);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format(money.amount()) + " " + money.currencyCode();
    }

    public static String signedMoney(MoneyDto money) {
        if (money == null || money.amount() == null) {
            return "—";
        }
        String sign = money.amount().signum() > 0 ? "+" : "";
        return sign + money(money);
    }

    public static String percent(PercentageDto percentage) {
        if (percentage == null || percentage.percent() == null) {
            return "—";
        }
        NumberFormat format = NumberFormat.getNumberInstance(DISPLAY_LOCALE);
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format.format(percentage.percent()) + " %";
    }

    public static String decimal(BigDecimal value) {
        if (value == null) {
            return "—";
        }
        NumberFormat format = NumberFormat.getNumberInstance(DISPLAY_LOCALE);
        format.setMaximumFractionDigits(8);
        return format.format(value);
    }

    public static String errorMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current == null ? "Unknown error" : current.getMessage();
    }
}
