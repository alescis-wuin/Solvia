package fr.seynax.solvia.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import fr.seynax.solvia.domain.common.DomainValidation;
import fr.seynax.solvia.domain.money.MoneyAmount;

public record AccountBalanceSnapshot(
        UUID id,
        UUID accountId,
        LocalDate valueDate,
        MoneyAmount balance,
        ValuationConfidence confidence,
        String note,
        Instant recordedAt
) {

    public AccountBalanceSnapshot {
        id = DomainValidation.requireId(id, "account snapshot id");
        accountId = DomainValidation.requireId(accountId, "account snapshot account id");
        valueDate = DomainValidation.requireDate(valueDate, "account snapshot value date");
        balance = Objects.requireNonNull(balance, "account snapshot balance is required");
        confidence = Objects.requireNonNull(confidence, "account snapshot confidence is required");
        note = note == null ? null : note.strip();
        recordedAt = DomainValidation.requireInstant(recordedAt, "account snapshot record date");
    }

    public static AccountBalanceSnapshot observed(UUID accountId, LocalDate valueDate, MoneyAmount balance) {
        return new AccountBalanceSnapshot(
                UUID.randomUUID(),
                accountId,
                valueDate,
                balance,
                ValuationConfidence.OBSERVED,
                null,
                Instant.now()
        );
    }
}
