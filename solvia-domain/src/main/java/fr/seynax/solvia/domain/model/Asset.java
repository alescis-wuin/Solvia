package fr.seynax.solvia.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import fr.seynax.solvia.domain.common.DomainValidation;
import fr.seynax.solvia.domain.money.CurrencyCode;

public record Asset(
        UUID id,
        String name,
        AssetType type,
        CurrencyCode currency,
        String symbol,
        boolean active,
        Instant createdAt
) {

    public Asset {
        id = DomainValidation.requireId(id, "asset id");
        name = DomainValidation.requireNonBlank(name, "asset name");
        type = Objects.requireNonNull(type, "asset type is required");
        currency = Objects.requireNonNull(currency, "asset currency is required");
        symbol = symbol == null ? null : symbol.strip();
        createdAt = DomainValidation.requireInstant(createdAt, "asset creation date");
    }

    public static Asset create(String name, AssetType type, CurrencyCode currency, String symbol) {
        return new Asset(UUID.randomUUID(), name, type, currency, symbol, true, Instant.now());
    }
}
