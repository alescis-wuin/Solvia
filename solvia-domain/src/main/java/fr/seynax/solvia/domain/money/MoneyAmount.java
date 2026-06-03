package fr.seynax.solvia.domain.money;

import java.math.BigDecimal;
import java.util.Objects;

public record MoneyAmount(BigDecimal amount, CurrencyCode currency) {

    public MoneyAmount {
        amount = Objects.requireNonNull(amount, "amount is required").stripTrailingZeros();
        currency = Objects.requireNonNull(currency, "currency is required");
    }

    public static MoneyAmount of(BigDecimal amount, CurrencyCode currency) {
        return new MoneyAmount(amount, currency);
    }

    public static MoneyAmount zero(CurrencyCode currency) {
        return new MoneyAmount(BigDecimal.ZERO, currency);
    }

    public MoneyAmount plus(MoneyAmount other) {
        requireSameCurrency(other);
        return new MoneyAmount(amount.add(other.amount), currency);
    }

    public MoneyAmount minus(MoneyAmount other) {
        requireSameCurrency(other);
        return new MoneyAmount(amount.subtract(other.amount), currency);
    }

    public MoneyAmount negate() {
        return new MoneyAmount(amount.negate(), currency);
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    private void requireSameCurrency(MoneyAmount other) {
        Objects.requireNonNull(other, "other amount is required");
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("money amounts must use the same currency");
        }
    }
}
