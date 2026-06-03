package fr.seynax.solvia.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import fr.seynax.solvia.domain.common.DomainValidation;
import fr.seynax.solvia.domain.money.MoneyAmount;

public record MarketPrice(
        UUID id,
        UUID assetId,
        LocalDate priceDate,
        MoneyAmount price,
        String source,
        Instant recordedAt
) {

    public MarketPrice {
        id = DomainValidation.requireId(id, "market price id");
        assetId = DomainValidation.requireId(assetId, "market price asset id");
        priceDate = DomainValidation.requireDate(priceDate, "market price date");
        price = Objects.requireNonNull(price, "market price value is required");
        if (price.isNegative() || price.isZero()) {
            throw new IllegalArgumentException("market price must be positive");
        }
        source = source == null ? null : source.strip();
        recordedAt = DomainValidation.requireInstant(recordedAt, "market price record date");
    }

    public static MarketPrice of(UUID assetId, LocalDate priceDate, MoneyAmount price, String source) {
        return new MarketPrice(UUID.randomUUID(), assetId, priceDate, price, source, Instant.now());
    }
}
