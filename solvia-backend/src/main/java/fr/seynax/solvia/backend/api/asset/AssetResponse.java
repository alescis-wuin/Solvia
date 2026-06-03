package fr.seynax.solvia.backend.api.asset;

import java.time.Instant;
import java.util.UUID;

import fr.seynax.solvia.domain.model.Asset;
import fr.seynax.solvia.domain.model.AssetType;

public record AssetResponse(
        UUID id,
        String name,
        AssetType type,
        String currencyCode,
        String symbol,
        boolean active,
        Instant createdAt
) {

    public static AssetResponse from(Asset asset) {
        return new AssetResponse(
                asset.id(),
                asset.name(),
                asset.type(),
                asset.currency().value(),
                asset.symbol(),
                asset.active(),
                asset.createdAt()
        );
    }
}
