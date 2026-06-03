package fr.seynax.solvia.domain.money;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Objects;

public record Percentage(BigDecimal ratio) {

    public Percentage {
        ratio = Objects.requireNonNull(ratio, "ratio is required").stripTrailingZeros();
    }

    public static Percentage ofRatio(BigDecimal ratio) {
        return new Percentage(ratio);
    }

    public static Percentage ofPercent(BigDecimal percent) {
        Objects.requireNonNull(percent, "percent is required");
        return new Percentage(percent.divide(BigDecimal.valueOf(100), MathContext.DECIMAL64));
    }

    public BigDecimal asPercent() {
        return ratio.multiply(BigDecimal.valueOf(100));
    }
}
