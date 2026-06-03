package fr.seynax.solvia.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import fr.seynax.solvia.domain.common.DomainValidation;

public record Position(
        UUID id,
        UUID accountId,
        UUID assetId,
        BigDecimal quantity,
        boolean active,
        Instant createdAt
) {

    public Position {
        id = DomainValidation.requireId(id, "position id");
        accountId = DomainValidation.requireId(accountId, "position account id");
        assetId = DomainValidation.requireId(assetId, "position asset id");
        quantity = DomainValidation.requireZeroOrPositive(quantity, "position quantity").stripTrailingZeros();
        createdAt = DomainValidation.requireInstant(createdAt, "position creation date");
    }

    public static Position create(UUID accountId, UUID assetId, BigDecimal quantity) {
        return new Position(UUID.randomUUID(), accountId, assetId, quantity, true, Instant.now());
    }

    public Position withQuantity(BigDecimal newQuantity) {
        return new Position(id, accountId, assetId, newQuantity, active, createdAt);
    }

    public Position deactivate() {
        return new Position(id, accountId, assetId, quantity, false, createdAt);
    }
}
