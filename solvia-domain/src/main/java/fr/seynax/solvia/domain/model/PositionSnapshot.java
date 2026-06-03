package fr.seynax.solvia.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import fr.seynax.solvia.domain.common.DomainValidation;
import fr.seynax.solvia.domain.money.MoneyAmount;

public record PositionSnapshot(
        UUID id,
        UUID positionId,
        LocalDate valueDate,
        BigDecimal quantity,
        MoneyAmount marketValue,
        ValuationConfidence confidence,
        Instant recordedAt
) {

    public PositionSnapshot {
        id = DomainValidation.requireId(id, "position snapshot id");
        positionId = DomainValidation.requireId(positionId, "position snapshot position id");
        valueDate = DomainValidation.requireDate(valueDate, "position snapshot value date");
        quantity = DomainValidation.requireZeroOrPositive(quantity, "position snapshot quantity").stripTrailingZeros();
        marketValue = Objects.requireNonNull(marketValue, "position snapshot market value is required");
        if (marketValue.isNegative()) {
            throw new IllegalArgumentException("position snapshot market value must not be negative");
        }
        confidence = Objects.requireNonNull(confidence, "position snapshot confidence is required");
        recordedAt = DomainValidation.requireInstant(recordedAt, "position snapshot record date");
    }

    public static PositionSnapshot observed(UUID positionId, LocalDate valueDate, BigDecimal quantity, MoneyAmount marketValue) {
        return new PositionSnapshot(
                UUID.randomUUID(),
                positionId,
                valueDate,
                quantity,
                marketValue,
                ValuationConfidence.OBSERVED,
                Instant.now()
        );
    }
}
