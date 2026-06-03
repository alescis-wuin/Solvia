package fr.seynax.solvia.backend.api.money;

import java.math.BigDecimal;

import fr.seynax.solvia.domain.money.MoneyAmount;

public record MoneyResponse(
        BigDecimal amount,
        String currencyCode
) {

    public static MoneyResponse from(MoneyAmount moneyAmount) {
        return new MoneyResponse(moneyAmount.amount(), moneyAmount.currency().value());
    }
}
