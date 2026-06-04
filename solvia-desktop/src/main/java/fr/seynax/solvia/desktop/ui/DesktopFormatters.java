package fr.seynax.solvia.desktop.ui;

import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.http.HttpTimeoutException;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.CompletionException;

import fr.seynax.solvia.desktop.api.ApiDtos.MoneyDto;
import fr.seynax.solvia.desktop.api.ApiDtos.PercentageDto;
import fr.seynax.solvia.desktop.api.SolviaApiException;

public final class DesktopFormatters {
    private static final Locale DISPLAY_LOCALE = Locale.FRANCE;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy")
            .withLocale(DISPLAY_LOCALE);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss")
            .withLocale(DISPLAY_LOCALE)
            .withZone(ZoneId.systemDefault());

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

    public static String date(LocalDate date) {
        return date == null ? "—" : DATE_FORMATTER.format(date);
    }

    public static String period(LocalDate from, LocalDate to) {
        return date(from) + " → " + date(to);
    }

    public static String time(Instant instant) {
        return instant == null ? "—" : TIME_FORMATTER.format(instant);
    }

    public static String assetType(String value) {
        if (value == null || value.isBlank()) {
            return "Non classé";
        }
        return switch (value) {
            case "FIAT_CURRENCY" -> "Liquidités";
            case "STOCK" -> "Actions";
            case "ETF" -> "ETF";
            case "BOND" -> "Obligations";
            case "CRYPTO_ASSET" -> "Crypto-actifs";
            case "PRIVATE_EQUITY" -> "Private equity";
            case "REAL_ESTATE" -> "Immobilier";
            case "CASHBACK_REWARD" -> "Cashback";
            case "OTHER" -> "Autres";
            default -> value;
        };
    }

    public static String aggregation(String value) {
        if (value == null || value.isBlank()) {
            return "—";
        }
        return switch (value.strip().toLowerCase(Locale.ROOT)) {
            case "last", "last_known" -> "Dernière valeur connue";
            case "average", "avg" -> "Moyenne journalière";
            default -> value;
        };
    }

    public static String bucket(String value) {
        if (value == null || value.isBlank()) {
            return "—";
        }
        String normalized = value.strip().toLowerCase(Locale.ROOT);
        if (normalized.length() < 2) {
            return value;
        }
        try {
            int amount = Integer.parseInt(normalized.substring(0, normalized.length() - 1));
            char unit = normalized.charAt(normalized.length() - 1);
            return switch (unit) {
                case 'd' -> amount + " jour" + plural(amount);
                case 'w' -> amount + " semaine" + plural(amount);
                case 'm' -> amount + " mois";
                default -> value;
            };
        } catch (NumberFormatException exception) {
            return value;
        }
    }

    public static String count(int value, String singular, String plural) {
        return value + " " + (value > 1 ? plural : singular);
    }

    public static String errorMessage(Throwable throwable) {
        Throwable current = unwrap(throwable);
        if (current == null) {
            return "Erreur inconnue.";
        }
        if (current instanceof SolviaApiException apiException) {
            return apiException.userMessage();
        }
        if (current instanceof HttpTimeoutException) {
            return "Le backend ne répond pas dans le délai attendu.";
        }
        if (current instanceof ConnectException) {
            return "Connexion impossible au backend local. Vérifier qu’il est démarré.";
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }

    public static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private static String plural(int value) {
        return value > 1 ? "s" : "";
    }
}
