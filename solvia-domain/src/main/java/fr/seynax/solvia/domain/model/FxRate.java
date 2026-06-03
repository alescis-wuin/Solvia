package fr.seynax.solvia.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import fr.seynax.solvia.domain.common.DomainValidation;
import fr.seynax.solvia.domain.money.CurrencyCode;

public record FxRate(
        UUID id,
        CurrencyCode baseCurrency,
        CurrencyCode quoteCurrency,
        LocalDate rateDate,
        BigDecimal rate,
        String source,
        Instant recordedAt
) {

    public FxRate {
        id = DomainValidation.requireId(id, "FX rate id");
        baseCurrency = Objects.requireNonNull(baseCurrency, "FX base currency is required");
        quoteCurrency = Objects.requireNonNull(quoteCurrency, "FX quote currency is required");
        if (baseCurrency.equals(quoteCurrency)) {
            throw new IllegalArgumentException("FX currencies must be different");
        }
        rateDate = DomainValidation.requireDate(rateDate, "FX rate date");
        rate = DomainValidation.requirePositive(rate, "FX rate").stripTrailingZeros();
        source = source == null ? null : source.strip();
        recordedAt = DomainValidation.requireInstant(recordedAt, "FX rate record date");
    }

    public static FxRate of(CurrencyCode baseCurrency, CurrencyCode quoteCurrency, LocalDate rateDate, BigDecimal rate, String source) {
        return new FxRate(UUID.randomUUID(), baseCurrency, quoteCurrency, rateDate, rate, source, Instant.now());
    }
}
