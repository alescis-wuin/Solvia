package fr.seynax.solvia.domain.common;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class DomainValidation {

    private DomainValidation() {
    }

    public static UUID requireId(UUID value, String fieldName) {
        return Objects.requireNonNull(value, fieldName + " is required");
    }

    public static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.strip();
    }

    public static BigDecimal requireNumber(BigDecimal value, String fieldName) {
        return Objects.requireNonNull(value, fieldName + " is required");
    }

    public static BigDecimal requirePositive(BigDecimal value, String fieldName) {
        BigDecimal checked = requireNumber(value, fieldName);
        if (checked.signum() <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return checked;
    }

    public static BigDecimal requireZeroOrPositive(BigDecimal value, String fieldName) {
        BigDecimal checked = requireNumber(value, fieldName);
        if (checked.signum() < 0) {
            throw new IllegalArgumentException(fieldName + " must be zero or positive");
        }
        return checked;
    }

    public static LocalDate requireDate(LocalDate value, String fieldName) {
        return Objects.requireNonNull(value, fieldName + " is required");
    }

    public static Instant requireInstant(Instant value, String fieldName) {
        return Objects.requireNonNull(value, fieldName + " is required");
    }
}
