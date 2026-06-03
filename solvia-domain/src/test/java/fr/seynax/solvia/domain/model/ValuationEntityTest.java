package fr.seynax.solvia.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import fr.seynax.solvia.domain.money.CurrencyCode;
import fr.seynax.solvia.domain.money.MoneyAmount;

class ValuationEntityTest {

    @Test
    void rejectsZeroCashFlowAmount() {
        assertThrows(IllegalArgumentException.class, () -> CashFlow.of(
                UUID.randomUUID(),
                CashFlowType.DEPOSIT,
                LocalDate.now(),
                MoneyAmount.zero(CurrencyCode.eur()),
                "Initial deposit"
        ));
    }

    @Test
    void rejectsNegativePositionMarketValue() {
        assertThrows(IllegalArgumentException.class, () -> new PositionSnapshot(
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDate.now(),
                BigDecimal.ONE,
                MoneyAmount.of(new BigDecimal("-1"), CurrencyCode.eur()),
                ValuationConfidence.OBSERVED,
                Instant.now()
        ));
    }

    @Test
    void rejectsFxRateWithSameCurrencies() {
        assertThrows(IllegalArgumentException.class, () -> FxRate.of(
                CurrencyCode.eur(),
                CurrencyCode.eur(),
                LocalDate.now(),
                BigDecimal.ONE,
                "test"
        ));
    }

    @Test
    void trimsMarketPriceSource() {
        MarketPrice price = MarketPrice.of(
                UUID.randomUUID(),
                LocalDate.now(),
                MoneyAmount.of(new BigDecimal("42"), CurrencyCode.eur()),
                " manual "
        );

        assertEquals("manual", price.source());
    }
}
