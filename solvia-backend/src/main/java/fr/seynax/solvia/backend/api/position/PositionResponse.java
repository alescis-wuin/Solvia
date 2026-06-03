package fr.seynax.solvia.backend.api.position;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import fr.seynax.solvia.domain.model.Position;

public record PositionResponse(
        UUID id,
        UUID accountId,
        UUID assetId,
        BigDecimal quantity,
        boolean active,
        Instant createdAt
) {

    public static PositionResponse from(Position position) {
        return new PositionResponse(
                position.id(),
                position.accountId(),
                position.assetId(),
                position.quantity(),
                position.active(),
                position.createdAt()
        );
    }
}
