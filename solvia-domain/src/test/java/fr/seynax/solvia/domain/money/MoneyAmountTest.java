package fr.seynax.solvia.domain.money;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class MoneyAmountTest {

    @Test
    void addsAmountsWithSameCurrency() {
        MoneyAmount first = MoneyAmount.of(new BigDecimal("100.00"), CurrencyCode.eur());
        MoneyAmount second = MoneyAmount.of(new BigDecimal("25.00"), CurrencyCode.eur());

        MoneyAmount result = first.plus(second);

        assertEquals(new BigDecimal("125"), result.amount());
        assertEquals(CurrencyCode.eur(), result.currency());
    }

    @Test
    void rejectsArithmeticAcrossCurrencies() {
        MoneyAmount eur = MoneyAmount.of(new BigDecimal("100"), CurrencyCode.eur());
        MoneyAmount usd = MoneyAmount.of(new BigDecimal("100"), CurrencyCode.usd());

        assertThrows(IllegalArgumentException.class, () -> eur.plus(usd));
    }
}
