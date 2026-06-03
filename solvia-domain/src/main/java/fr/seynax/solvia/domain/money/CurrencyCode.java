package fr.seynax.solvia.domain.money;

import java.util.Locale;

public record CurrencyCode(String value) {

    public CurrencyCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("currency code must not be blank");
        }

        value = value.strip().toUpperCase(Locale.ROOT);

        if (!value.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("currency code must use three ISO-like letters");
        }
    }

    public static CurrencyCode of(String value) {
        return new CurrencyCode(value);
    }

    public static CurrencyCode eur() {
        return new CurrencyCode("EUR");
    }

    public static CurrencyCode usd() {
        return new CurrencyCode("USD");
    }
}
