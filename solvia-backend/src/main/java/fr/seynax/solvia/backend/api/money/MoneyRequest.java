package fr.seynax.solvia.backend.api.money;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import fr.seynax.solvia.domain.money.CurrencyCode;
import fr.seynax.solvia.domain.money.MoneyAmount;

public record MoneyRequest(
        @NotNull BigDecimal amount,
        @NotBlank String currencyCode
) {

    public MoneyAmount toMoneyAmount() {
        return MoneyAmount.of(amount, CurrencyCode.of(currencyCode));
    }
}
