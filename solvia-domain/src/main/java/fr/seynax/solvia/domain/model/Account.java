package fr.seynax.solvia.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import fr.seynax.solvia.domain.common.DomainValidation;
import fr.seynax.solvia.domain.money.CurrencyCode;

public record Account(
        UUID id,
        String name,
        AccountType type,
        EnvelopeType envelopeType,
        CurrencyCode currency,
        boolean active,
        Instant createdAt
) {

    public Account {
        id = DomainValidation.requireId(id, "account id");
        name = DomainValidation.requireNonBlank(name, "account name");
        type = Objects.requireNonNull(type, "account type is required");
        envelopeType = Objects.requireNonNull(envelopeType, "envelope type is required");
        currency = Objects.requireNonNull(currency, "account currency is required");
        createdAt = DomainValidation.requireInstant(createdAt, "account creation date");
    }

    public static Account create(String name, AccountType type, EnvelopeType envelopeType, CurrencyCode currency) {
        return new Account(UUID.randomUUID(), name, type, envelopeType, currency, true, Instant.now());
    }

    public Account rename(String newName) {
        return new Account(id, newName, type, envelopeType, currency, active, createdAt);
    }

    public Account deactivate() {
        return new Account(id, name, type, envelopeType, currency, false, createdAt);
    }
}
