package fr.seynax.solvia.backend.api.calculation;

import java.math.BigDecimal;

import fr.seynax.solvia.domain.money.Percentage;

public record PercentageResponse(
        BigDecimal ratio,
        BigDecimal percent
) {

    public static PercentageResponse from(Percentage percentage) {
        return new PercentageResponse(percentage.ratio(), percentage.asPercent());
    }
}
