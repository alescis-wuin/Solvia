package fr.seynax.solvia.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import fr.seynax.solvia.domain.common.DomainValidation;
import fr.seynax.solvia.domain.money.MoneyAmount;

public record CashFlow(
        UUID id,
        UUID accountId,
        CashFlowType type,
        LocalDate valueDate,
        MoneyAmount amount,
        String label,
        Instant recordedAt
) {

    public CashFlow {
        id = DomainValidation.requireId(id, "cash flow id");
        accountId = DomainValidation.requireId(accountId, "cash flow account id");
        type = Objects.requireNonNull(type, "cash flow type is required");
        valueDate = DomainValidation.requireDate(valueDate, "cash flow value date");
        amount = Objects.requireNonNull(amount, "cash flow amount is required");
        if (amount.isZero()) {
            throw new IllegalArgumentException("cash flow amount must not be zero");
        }
        label = label == null ? null : label.strip();
        recordedAt = DomainValidation.requireInstant(recordedAt, "cash flow record date");
    }

    public static CashFlow of(UUID accountId, CashFlowType type, LocalDate valueDate, MoneyAmount amount, String label) {
        return new CashFlow(UUID.randomUUID(), accountId, type, valueDate, amount, label, Instant.now());
    }
}
