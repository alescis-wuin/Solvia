package fr.seynax.solvia.desktop.ui;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Pattern;

public final class InputValidation {

    private static final Pattern CURRENCY_PATTERN = Pattern.compile("[A-Z]{3}");

    private InputValidation() {
    }

    public static ValidationResult<BigDecimal> amount(String value, String fieldName) {
        String label = fieldName == null || fieldName.isBlank() ? "Montant" : fieldName;
        if (value == null || value.isBlank()) {
            return ValidationResult.error(label + " obligatoire.");
        }
        try {
            BigDecimal parsed = new BigDecimal(value.strip().replace(',', '.'));
            return ValidationResult.ok(parsed.stripTrailingZeros());
        } catch (RuntimeException exception) {
            return ValidationResult.error(label + " invalide. Exemple attendu: 1234.56");
        }
    }

    public static ValidationResult<String> positiveAmount(String value, String fieldName) {
        ValidationResult<BigDecimal> result = amount(value, fieldName);
        if (!result.valid()) {
            return ValidationResult.error(result.message());
        }
        if (result.value().signum() <= 0) {
            return ValidationResult.error(fieldName + " doit etre strictement positif.");
        }
        return ValidationResult.ok(result.value().toPlainString());
    }

    public static ValidationResult<String> currencyCode(String value, String fieldName) {
        String label = fieldName == null || fieldName.isBlank() ? "Devise" : fieldName;
        if (value == null || value.isBlank()) {
            return ValidationResult.error(label + " obligatoire.");
        }
        String normalized = value.strip().toUpperCase(Locale.ROOT);
        if (!CURRENCY_PATTERN.matcher(normalized).matches()) {
            return ValidationResult.error(label + " invalide. Utiliser trois lettres ISO, par exemple EUR ou USD.");
        }
        return ValidationResult.ok(normalized);
    }

    public record ValidationResult<T>(boolean valid, T value, String message) {
        public static <T> ValidationResult<T> ok(T value) {
            return new ValidationResult<>(true, value, null);
        }

        public static <T> ValidationResult<T> error(String message) {
            return new ValidationResult<>(false, null, message);
        }
    }
}
